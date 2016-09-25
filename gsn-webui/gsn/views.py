import csv
import json
from datetime import datetime, timedelta
import requests
import re
from django.conf import settings
from django.contrib.auth import login, logout
from django.contrib.auth.decorators import login_required
from django.http import HttpResponse, JsonResponse, HttpResponseRedirect, HttpResponseNotFound
from django.http import HttpResponseForbidden
from django.shortcuts import redirect
from django.template import loader
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from gsn.models import GSNUser

# Server adress and services


# OAUTH2
oauth_client_id = settings.GSN['CLIENT_ID']
oauth_client_secret = settings.GSN['CLIENT_SECRET']
oauth_redirection_url = settings.GSN['WEBUI_URL'] + "profile/"
oauth_sensors_url = settings.GSN['SERVICE_URL_LOCAL'] + "api/sensors"
oauth_auth_url = settings.GSN['SERVICE_URL_PUBLIC'] + "oauth2/auth"
oauth_token_url = settings.GSN['SERVICE_URL_LOCAL'] + "oauth2/token"
oauth_user_url = settings.GSN['SERVICE_URL_LOCAL'] + "api/user"
api_websocket = re.sub(r"http(s)?://", "ws://", settings.GSN['SERVICE_URL_PUBLIC'])
max_query_size = settings.GSN['MAX_QUERY_SIZE']


# Views

def index(request):
    """
    Renders the base template of the app
    """
    if request.user.is_authenticated():
        context = {
            'log_page': 'logout',
            'logged_in': 'true',
            'user': request.user.username,
            'ws_url': api_websocket
        }

    else:
        context = {
            'log_page': 'login',
            'logged_out': 'true',
            'ws_url': api_websocket
        }

    template = loader.get_template('gsn/index.html')
    response = HttpResponse(template.render(context))
    return response


def sensors(request):
    """
    Return the list of sensors as gotten from the GSN server
    """

    payload = {
        'latestValues': True,
    }

    if request.user.is_authenticated():
        return JsonResponse(
            json.loads(requests.get(oauth_sensors_url, params=payload, headers=create_headers(request.user)).text))
    else:
        return JsonResponse(json.loads(requests.get(oauth_sensors_url).text))


@login_required
def dashboard(request, sensor_name):
    if len(request.user.favorites) < 1:
        return HttpResponseNotFound()

    data = {}

    payload = {
        'latestValues': True,
    }

    if sensor_name in request.user.favorites:
        r = requests.get(oauth_sensors_url + '/' + sensor_name, params=payload,
                         headers=create_headers(request.user))
        sensor_data = json.loads(r.text)

        data = {
            'values': sensor_data['properties']['values'][0],
            'geographical': sensor_data['properties']['geographical'],
            'fields': sensor_data['properties']['fields']
        }

        data['values'][0] = datetime.fromtimestamp(data['values'][0] / 1000).isoformat(sep='T')

        return JsonResponse(data)

    return HttpResponseNotFound()


@login_required
def favorites_list(request):
    list = []

    for key, value in request.user.favorites.items():
        list.append(key)

    if len(list) > 0:
        return JsonResponse({
            'favorites_list': list
        })
    else:
        return HttpResponseNotFound()


@login_required
def favorites_manage(request):
    add = request.GET.get('add')

    if add is not None:
        request.user.favorites[add] = ''
        request.user.save()
        return HttpResponse('added')

    remove = request.GET.get('remove')

    if remove is not None:
        try:
            request.user.favorites.pop(remove)
            request.user.save()
        except KeyError:
            pass
        return HttpResponse('removed')

    return HttpResponseNotFound()


def sensor_detail(request, sensor_name, from_date, to_date):
    """
    Returns the details of a sensor and its values for a specified time frame in iso8601. Adds a time value to the
    data.

    If the user is logged out, returns the details stripped from the value field.
    """

    if request.user.is_authenticated():

        headers = create_headers(request.user)

        user_data = {
            'logged': True,
            'has_access': True,
            'favorite': sensor_name in request.user.favorites
        }

        payload = {
            'from': from_date,
            'to': to_date
        }

        r = requests.get(oauth_sensors_url + '/' + sensor_name + '/data', headers=headers, params=payload)

        if r.status_code is not 200:

            r = requests.get(oauth_sensors_url + '/' + sensor_name, headers=headers)
            user_data.update({
                'has_access': False
            })

            if r.status_code is not 200:
                return JsonResponse({
                    'error': 'The specified sensor doesn\'t exist'
                })

        data = json.loads(r.text)

        data = add_time(data)

        data.update({
            'user': user_data
        })

        return JsonResponse(data)

    else:

        user_data = {
            'logged': False,
            'has_access': False
        }

        payload = {
            'latestValues': False
        }

        r = requests.get(oauth_sensors_url + '/' + sensor_name, params=payload)

        if r.status_code is not 200:
            return JsonResponse({
                'error': 'The specified sensor doesn\'t exist'
            })

        data = json.loads(r.text)

        data.update({
            'user': user_data
        })

        data['properties'].update({
            'values': []
        })

        return JsonResponse(data)


@login_required
def download_csv(request, sensor_name, from_date, to_date):
    """
    Create a CSV out of the sensor data then sends it to the client to be downloaded
    """

    payload = {
        'from': from_date,
        'to': to_date,
        'size': max_query_size
    }

    headers = create_headers(request.user)

    data = json.loads(requests.get(oauth_sensors_url + '/' + sensor_name + '/data', headers=headers, params=payload))

    data = add_time(data)

    response = HttpResponse(content_type='text/csv')
    response['Content-Disposition'] = 'attachment; filename="' + sensor_name + '.csv"'

    writer = csv.writer(response)

    if 'error' in data:
        return HttpResponseForbidden()

    writer.writerow([field['name'] + " (" + (field['unit'] if field['unit'] is not None else 'no unit') + " " + (
        field['type'] if field['type'] is not None else 'no type') + ")" for field in data['properties']['fields']])

    if 'values' in data['properties']:
        for value in data['properties']['values']:
            writer.writerow(value)
    else:
        writer.writerow(["No data for the selected timespan: " + from_date + ", " + to_date])

    return response


@csrf_exempt
@login_required
def download(request):
    """
    Create a CSV out of POST data sent by the client then send it for download
    """
    # TODO: Find a way to send the csrf token to the client beforehand

    data = json.loads(request.body.decode('utf-8'))

    response = HttpResponse(content_type='text/csv')
    response['Content-Disposition'] = 'attachment; filename="download.csv"'

    writer = csv.writer(response)
    writer.writerow([field['name'] + " (" + (field['unit'] if field['unit'] is not None else 'no unit') + " " + (
        field['type'] if field['type'] is not None else 'no type') + ")" for field in data['properties']['fields']])

    for value in data['properties']['values']:
        writer.writerow(value)

    return response


def logout_view(request):
    """
    Logs out the user then redirected them to the / page
    """
    logout(request)
    return redirect('index')


def profile(request):

    code = request.GET.get('code')
    if code is not None:
        user = get_or_create_user(code)
        if user:
            user.backend = 'django.contrib.auth.backends.ModelBackend'
            login(request, user)
    return redirect('index')


def oauth_get_code(request):
    """
    Redirects to the oauth server
    """
    return HttpResponseRedirect(oauth_auth_url + '?' + 'response_type=code' + '&client_id=' + oauth_client_id + '&client_secret=' + oauth_client_secret)


def create_headers(user):
    return {
        'Authorization': 'Bearer ' + get_or_refresh_token(user)
    }


def get_or_refresh_token(user):
    if user.refresh_token is None:
        return ''
    if timezone.now() > user.token_expire_date or user.token_expire_date is None:
        return refresh_token(user)
    else:
        return user.access_token


def refresh_token(user):
    payload = {
        'client_id': oauth_client_id,
        'client_secret': oauth_client_secret,
        'redirect_uri': oauth_redirection_url,
        'refresh_token': user.refresh_token,
        'grant_type': 'refresh_token'
    }

    r = requests.post(oauth_token_url, params=payload)

    data = r.json()

    user.access_token = data['access_token']
    user.refresh_token = data['refresh_token']
    user.token_created_date = timezone.now()
    user.token_expire_date = user.token_created_date + timedelta(seconds=data['expires_in'])

    user.save()

    return user.access_token


def get_or_create_user(code):
    payload = {
        'client_id': oauth_client_id,
        'client_secret': oauth_client_secret,
        'redirect_uri': oauth_redirection_url,
        'code': code,
        'grant_type': 'authorization_code'
    }

    data = requests.post(oauth_token_url, data=payload)

    data = json.loads(data.text)

    if "error" in data:
        return None

    access_token = 'Bearer ' + data["access_token"]

    headers = {
        "Authorization": access_token
    }

    # TODO: WAIT FOR USER INF TO RETURN STRICT JSON

    user_inf = requests.get(oauth_user_url, headers=headers).json()

    if not GSNUser.objects.filter(username=user_inf['username'], email=user_inf['email']).exists():
        user = GSNUser.objects.create_user(user_inf['username'], user_inf['email'], GSNUser.objects.make_random_password())
    else:
        user = GSNUser.objects.get(username=user_inf['username'], email=user_inf['email'])

    user.access_token = data['access_token']
    user.refresh_token = data['refresh_token']
    user.token_created_date = timezone.now()
    user.token_expire_date = user.token_created_date + timedelta(seconds=data['expires_in'])
    user.save()

    return user


def add_time(data):
    for idx, values in enumerate(data['properties']['values']):
        d = datetime.fromtimestamp(values[0] / 1000)
        data['properties']['values'][idx].insert(0, d.isoformat('T'))

    data['properties']['fields'].insert(0, {
        "unit": "",
        "name": "time",
        "type": "time"
    })

    return data
