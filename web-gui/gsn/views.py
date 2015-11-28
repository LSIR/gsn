import json
from datetime import datetime, timedelta
from django.utils import timezone
from django.shortcuts import redirect, render
from django.views.generic import TemplateView
from django.template.context_processors import csrf
from django.http import HttpResponse, JsonResponse, HttpResponseRedirect
from django.template import loader
import requests
import requests_cache
from django.views.decorators.csrf import csrf_exempt
from django.http import HttpResponseForbidden
from django.conf import settings
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import user_passes_test
from gsn.forms import GSNUserCreationForm, ProfileForm, LoginForm
from gsn.models import GSNUser
import csv

# Server adress and services

# server_address = "http://montblanc.slf.ch:22001"
server_address = settings.GSN['SERVER_URL']
sensors_url = settings.GSN['SENSORS_URL']

# OAUTH2
oauth_enabled = True  # TODO delete and uncomment when OAUTH ready
# oauth_enabled = settings.GSN['OAUTH']['ENABLED']
if oauth_enabled:
    oauth_server_address = settings.GSN['OAUTH']['SERVER_URL']
    oauth_client_id = settings.GSN['OAUTH']['CLIENT_ID']
    oauth_client_secret = settings.GSN['OAUTH']['CLIENT_SECRET']
    oauth_redirection_url = settings.GSN['OAUTH']['REDIRECTION_URL']
    oauth_sensors_url = settings.GSN['OAUTH']['SENSORS_URL']
    oauth_auth_url = settings.GSN['OAUTH']['AUTH_URL']
    oauth_token_url = settings.GSN['OAUTH']['TOKEN_URL']

# Cache setup
requests_cache.install_cache("demo_cache")


def gsn_user_check(user):
    return user.is_authenticated() and GSNUser.objects.filter(id=user.id).exists()


# Views



# Index view, just renders a template
def index(request):
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
    context.update({'login_form': LoginForm()})

    template = loader.get_template('gsn/index.html')
    response = HttpResponse(template.render(context))
    response.set_cookie('test_cookie', 'test_value')
    return response


# View that returns in JSON a list of all the sensors
def sensors(request):
    if request.user.is_authenticated() and GSNUser.objects.filter(id=request.user.id).exists():
        return JsonResponse(json.loads(requests.get(oauth_sensors_url, headers={
            'Authorization': 'Bearer ' + get_or_refresh_token(GSNUser.objects.get(id=request.user.id))
        }).text))
    else:
        return JsonResponse(json.loads(requests.get(sensors_url).text))


# View that gets the data of a sensor for a specified timeframe

def sensor_detail(request, sensor_name, from_date, to_date):
    # _from_ = str(datetime.now().replace(microsecond=0).isoformat(sep='T'))
    # _to_ = _from_

    if (oauth_enabled) and request.user.is_authenticated() and GSNUser.objects.filter(id=request.user.id).exists():

        headers = {
            'Authorization': 'Bearer ' + get_or_refresh_token(GSNUser.objects.get(id=request.user.id))
        }

        payload = {
            'from': from_date,
            'to': to_date,
        }

        data = json.loads(
            requests.get(oauth_sensors_url + '/' + sensor_name + '/data', headers=headers, params=payload).text)

        for index, values in enumerate(data['properties']['values']):
            # data['properties']['values'][index] = [float(i) for i in values]
            d = datetime.fromtimestamp(values[0] / 1000)
            data['properties']['values'][index].insert(0, d.isoformat('T'))

        data['properties']['fields'].insert(0, {
            "unit": "",
            "name": "time",
            "type": "time"
        })

        def edit_data(values):

            pass

        dict = {
            'features': [data, ]
        }

        return JsonResponse(dict)

    else:
        payload = {
            'from': from_date,
            'to': to_date,
            'username': 'john',
            'password': 'john'
        }

        return JsonResponse(
            json.loads(requests.get(sensors_url + sensor_name + '/', params=payload).text))


# View that downloads the data of a sensor for the specified time frame
def download_csv(request, sensor_name, from_date, to_date):
    if (oauth_enabled) and request.user.is_authenticated() and GSNUser.objects.filter(id=request.user.id).exists():

        # TODO: complete for OAUTH
        pass
    else:
        payload = {
            'from': from_date,
            'to': to_date,
            'username': 'john',
            'password': 'john'
        }

        data = json.loads(requests.get(sensors_url + sensor_name + '/', params=payload).text)

        response = HttpResponse(content_type='text/csv')
        response['Content-Disposition'] = 'attachment; filename="' + sensor_name + '.csv"'

        writer = csv.writer(response)

        if 'error' in data:
            return HttpResponseForbidden()

        writer.writerow([field['name'] + " (" + field['unit'] + " " + field['type'] + ")"
                         for field in data['features'][0]['properties']['fields']])

        if 'values' in data['features'][0]['properties']:
            for value in data['features'][0]['properties']['values']:
                writer.writerow(value)
        else:
            writer.writerow(["No data for the selected timespan: " + from_date + ", " + to_date])

        return response


@csrf_exempt
def download(request):
    data = json.loads(
        request.body.decode('utf-8')
    )

    response = HttpResponse(content_type='text/csv')
    response['Content-Disposition'] = 'attachment; filename="download.csv"'

    writer = csv.writer(response)
    writer.writerow(
        [field['name'] + " (" + (field['unit'] if field['unit'] is not None else 'no unit') + " " + (
            field['type'] if field['type'] is not None else 'no type') + ")"
         for field in data['properties']['fields']])

    for value in data['properties']['values']:
        writer.writerow(value)

    return response


def login_request(request):
    context = {'form': LoginForm(), 'next': request.GET.get('next')}
    context.update(csrf(request))

    if request.method == "POST":

        form = LoginForm(data=request.POST)

        if form.is_valid:
            username = request.POST.get('username')
            password = request.POST.get('password')
            user = authenticate(username=username, password=password)
            if user is not None:
                if user.is_active:
                    login(request, user)

                    if request.GET.get('next') is not None:
                        return redirect(request.GET.get('next'))

                    return redirect('index')
                else:
                    context.update({
                        'user_desactivated': 'This user has been desactivated',
                    })
            else:
                context.update({
                    'user_does_not_exist': 'This username and password combination is not valid',

                })
        else:
            context.update({
                'invalid_form': 'You didn\'t fill the form correctly',
            })

    return render(request, 'gsn/login.html', context)


def sign_up(request):
    if request.user.is_authenticated():
        logout(request)

    context = {}

    context.update(csrf(request))

    if request.method == "POST":

        form = GSNUserCreationForm(request.POST)

        if form.is_valid():
            form.save()
            context.update({'user_created': True})
            # login(request)

        else:
            context.update({'form_invalid': True})


    else:
        form = GSNUserCreationForm()

    context.update({'form': form})

    return render(request, 'gsn/sign_up.html', context)


def logout_view(request):
    logout(request)
    return redirect('index')


@user_passes_test(gsn_user_check)
def profile(request):
    context = {
        'username': request.user.username
    }

    context.update(csrf(request))

    if not GSNUser.objects.filter(id=request.user.id):
        user = request.user
    else:
        user = GSNUser.objects.get(id=request.user.id)

        code = request.GET.get('code')
        if code is not None:
            create_token(code, user)
            return redirect('profile')

    if request.method == "POST":

        form = ProfileForm(data=request.POST, instance=user)

        if form.is_valid():
            user.email = form.cleaned_data['email']
            user.save()
            form = ProfileForm(instance=user)


    else:
        form = ProfileForm(instance=user)

    context.update({'password_form': form})

    return render(request, 'gsn/profile.html', context)


def oauth_get_code(request):
    return HttpResponseRedirect(
        oauth_auth_url + '?' + 'response_type=code' +
        '&client_id=' + oauth_client_id +
        '&client_secret=' + oauth_client_secret)


def get_or_refresh_token(user):
    if timezone.now() > user.token_expire_date:
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

    json = requests.post(oauth_token_url, params=payload)

    data = json.json()

    user.access_token = data['access_token']
    user.refresh_token = data['refresh_token']
    user.token_created_date = datetime.now()
    user.token_expire_date = user.token_created_date + timedelta(seconds=data['expires_in'])

    user.save()

    return user.access_token


def create_token(code, user):
    payload = {
        'client_id': oauth_client_id,
        'client_secret': oauth_client_secret,
        'redirect_uri': oauth_redirection_url,
        'code': code,
        'grant_type': 'authorization_code'
    }

    data = requests.post(oauth_token_url, data=payload)

    data = data.json()

    user.access_token = data['access_token']
    user.refresh_token = data['refresh_token']
    user.token_created_date = datetime.now()
    user.token_expire_date = user.token_created_date + timedelta(seconds=data['expires_in'])
    user.save()

    return user.access_token
