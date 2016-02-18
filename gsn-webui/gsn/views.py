import csv
import json
from datetime import datetime, timedelta
import requests
from django.conf import settings
from django.contrib.auth import login, logout
from django.contrib.auth.decorators import login_required
from django.http import HttpResponse, JsonResponse, HttpResponseRedirect, HttpResponseNotFound
from django.http import HttpResponseForbidden
from django.shortcuts import redirect
from django.template import loader
from django.template.context_processors import csrf
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from gsn.forms import LoginForm
from django.contrib.auth.models import User

# Server adress and services


# OAUTH2
oauth_server_address = settings.GSN['SERVER_URL']
oauth_client_id = settings.GSN['CLIENT_ID']
oauth_client_secret = settings.GSN['CLIENT_SECRET']
oauth_redirection_url = settings.GSN['REDIRECTION_URL']
oauth_sensors_url = settings.GSN['SENSORS_URL']
oauth_auth_url = settings.GSN['AUTH_URL']
oauth_token_url = settings.GSN['TOKEN_URL']
oauth_user_url = settings.GSN['USER_INF_URL']
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
            'user': request.user.username
        }

    else:
        context = {
            'log_page': 'login',
            'logged_out': 'true',
        }

    context.update(csrf(request))
    context.update(dict(login_form=LoginForm()))

    template = loader.get_template('gsn/index.html')
    response = HttpResponse(template.render(context))
    response.set_cookie('test_cookie', 'test_value')
    return response


def sensors(request):
    """
    Return the list of sensors as gotten from the GSN server
    """
    if request.user.is_authenticated():
        return JsonResponse(
            json.loads(requests.get(oauth_sensors_url, headers=create_headers(request.user.gsnuser)).text))
    else:
        return JsonResponse(json.loads(requests.get(oauth_sensors_url).text))


@login_required
def dashboard(request, sensor_name):
    if len(request.user.gsnuser.favorites) < 1:
        return HttpResponseNotFound()

    data = {}

    payload = {
        'latestValues': True,
    }

    if sensor_name in request.user.gsnuser.favorites:
        r = requests.get(oauth_sensors_url + '/' + sensor_name, params=payload,
                         headers=create_headers(request.user.gsnuser))
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

    for key, value in request.user.gsnuser.favorites.items():
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
        request.user.gsnuser.favorites[add] = ''
        request.user.gsnuser.save()
        return HttpResponse('added')

    remove = request.GET.get('remove')

    if remove is not None:
        try:
            request.user.gsnuser.favorites.pop(remove)
            request.user.gsnuser.save()
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

        headers = create_headers(request.user.gsnuser)

        user_data = {
            'logged': True,
            'has_access': True,
            'favorite': sensor_name in request.user.gsnuser.favorites
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

    headers = create_headers(request.user.gsnuser)

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


# def login_request(request):
#     """
#     Log in the user from a LoginForm, assuming the credentials are correct. If they are, the user will be
# redirected to
#     the index page
#
#     If the credentials are incorrect or the user is inactive or the form is invalid, the user will be redirected to
#  the
#     /gsn/login/ page, with the appropriate error message.
#
#     If the manages to log in successfuly, the function checks if there is a 'next' parameter; if there is, the user is
#     redirected to that page. If not, he is redirected to /gsn/
#     """
#
#     context = {
#         'next': request.GET.get('next')
#     }
#     context.update(csrf(request))
#
#     if request.method == "POST":
#
#         form = LoginForm(data=request.POST)
#         context.update({
#             'form': form
#         })
#
#         if form.is_valid:
#             username = request.POST.get('username')
#             password = request.POST.get('password')
#             user = authenticate(username=username, password=password)
#             if user is not None:
#                 if user.is_active:
#                     login(request, user)
#
#                     if request.GET.get('next') is not None:
#                         return redirect(request.GET.get('next'))
#
#                     return redirect('index')
#                 """
#                 else:
#                     context.update({
#                         'error_message': 'This user has been desactivated'
#                     })
#             else:
#                 context.update({
#                     'error_message': 'This username and password combination is not valid'
#
#                 })
#         else:
#             context.update({
#                 'error_message': 'You didn\'t fill the form correctly'
#             })
#             """
#     else:
#         context.update({
#             'form': LoginForm(request)
#         })
#         pass
#
#     return render(request, 'gsn/login.html', context)


# def sign_up(request):
#     """
#     View that lets a user sign up for the website.
#
#     Ask for username, email and password using a GSNUserCreationForm. If the form is valid, the user is created, then
#     the login page is rendered and the user is prompted to log in.
#
#     If a user was already logged in, he will be logged out before this view is rendered.
#     """
#
#
#     if request.user.is_authenticated():
#         logout(request)
#
#     context = {}
#
#     context.update(csrf(request))
#
#     if request.method == "POST":
#
#         form = GSNUserCreationForm(request.POST)
#
#         if form.is_valid():
#             form.save()
#
#             context.update({
#                 'success_message': 'Your account was successfuly created. Please proceed to login.',
#                 'form': LoginForm()
#             })
#
#             return render(request, 'gsn/login.html', context)
#
#             # login(request)
#
#         else:
#
#             context.update({
#                 'error_message': 'A problem happened while submitting this form. It may be that this '
#                                  'username is already taken, that the passwords don\'t match or that '
#                                  'the '
#                                  'constraint on '
#                                  'the fields are not respected.'
#             })
#
#     else:
#         form = GSNUserCreationForm()
#
#     context.update({
#         'form': form
#     })
#
#     return render(request, 'gsn/sign_up.html', context)


def logout_view(request):
    """
    Logs out the user then redirected them to the /gsn/ page
    """
    logout(request)
    return redirect('index')


def profile(request):
    """
    View
    """

    code = request.GET.get('code')
    if code is not None:
        user = get_or_create_user(code)
        user.backend = 'django.contrib.auth.backends.ModelBackend'
        login(request, user)
        return redirect('profile')

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

    access_token = 'Bearer ' + data["access_token"]

    headers = {
        "Authorization": access_token
    }

    # TODO: WAIT FOR USER INF TO RETURN STRICT JSON

    user_inf = requests.get(oauth_user_url, headers=headers).json()

    if not User.objects.filter(username=user_inf['username'], email=user_inf['email']).exists():
        user = User.objects.create_user(user_inf['username'], user_inf['email'], User.objects.make_random_password())
    else:
        user = User.objects.get(username=user_inf['username'], email=user_inf['email'])

    user.save()

    gsnuser = user.gsnuser
    gsnuser.access_token = data['access_token']
    gsnuser.refresh_token = data['refresh_token']
    gsnuser.token_created_date = timezone.now()
    gsnuser.token_expire_date = gsnuser.token_created_date + timedelta(seconds=data['expires_in'])
    gsnuser.save()

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
