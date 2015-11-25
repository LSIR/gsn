import json
from datetime import datetime, timedelta
from django.shortcuts import redirect, render
from django.views.generic import TemplateView
from django.template.context_processors import csrf
from django.http import HttpResponse, JsonResponse
from django.template import loader
import requests
import requests_cache
from django.views.decorators.csrf import csrf_exempt
from django.http import HttpResponseForbidden
from django.conf import settings
from django.contrib.auth import authenticate, login, logout
from gsn.forms import GSNUserCreationForm, ProfileForm
from gsn.models import GSNUser
import csv

# Server adress and services

# server_address = "http://montblanc.slf.ch:22001"
server_address = settings.GSN['SERVER_URL']
sensor_suffix = settings.GSN['SUFFIXES']['SENSORS']

# OAUTH2
oauth_enabled = False  # TODO delete and uncomment when OAUTH ready
# oauth_enabled = settings.GSN['OAUTH']['ENABLED']
if oauth_enabled:
    oauth_server_address = settings.GSN['OAUTH']['SERVER_URL']
    oauth_client_id = settings.GSN['OAUTH']['CLIENT_ID']
    oauth_client_secret = settings.GSN['OAUTH']['CLIENT_SECRET']
    oauth_redirection_url = settings.GSN['OAUTH']['REDIRECTION_URL']
    sensor_suffix = settings.GSN['OAUTH']['SENSORS_SUFFIX']
    oauth_auth_suffix = settings.GSN['OAUTH']['AUTH_SUFFIX']
    oauth_token_suffix = settings.GSN['OAUTH']['TOKEN_SUFFIX']

# Cache setup
requests_cache.install_cache("demo_cache")


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

    template = loader.get_template('gsn/index.html')
    response = HttpResponse(template.render(context))
    response.set_cookie('test_cookie', 'test_value')
    return response


# View that returns in JSON a list of all the sensors
def sensors(request):
    return JsonResponse(json.loads(requests.get(server_address + sensor_suffix).text))


# View that gets the data of a sensor for a specified timeframe
def sensor_detail(request, sensor_name, from_date, to_date):
    # _from_ = str(datetime.now().replace(microsecond=0).isoformat(sep='T'))
    # _to_ = _from_

    if (oauth_enabled):
        # TODO: complete for OAUTH
        pass
    else:
        payload = {
            'from': from_date,
            'to': to_date,
            'username': 'john',
            'password': 'john'
        }

        return JsonResponse(
            json.loads(requests.get(server_address + sensor_suffix + '/' + sensor_name + '/', params=payload).text))


# View that downloads the data of a sensor for the specified time frame
def download_csv(request, sensor_name, from_date, to_date):
    if (oauth_enabled):
        # TODO: complete for OAUTH
        pass
    else:
        payload = {
            'from': from_date,
            'to': to_date,
            'username': 'john',
            'password': 'john'
        }

        data = json.loads(requests.get(server_address + sensor_suffix + '/' + sensor_name + '/', params=payload).text)

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
# View that downloads the data of multiple sensors for the specified time frame
def download(request):
    data = json.loads(
        request.body.decode('utf-8')
    )

    response = HttpResponse(content_type='text/csv')
    response['Content-Disposition'] = 'attachment; filename="download.csv"'

    writer = csv.writer(response)
    writer.writerow([field['name'] + " (" + field['unit'] + " " + field['type'] + ")"
                     for field in data['properties']['fields']])

    for value in data['properties']['values']:
        writer.writerow(value)

    return response


def login_request(request):
    username = request.POST.get('username')
    password = request.POST.get('password')
    user = authenticate(username=username, password=password)

    if user is not None:
        if user.is_active:
            login(request, user)
            return redirect('index')
        else:
            # TODO: add refused login
            pass
    else:
        # TODO: add feedback about incorrect credentials
        pass


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

        else:
            context.update({'form_invalid': True})


    else:
        form = GSNUserCreationForm()

    context.update({'form': form})

    return render(request, 'gsn/sign_up.html', context)


def logout_view(request):
    logout(request)
    return redirect('index')


def profile(request):
    context = {}

    if not request.user.is_authenticated():
        return HttpResponseForbidden()

    context.update(csrf(request))

    if not GSNUser.objects.filter(id=request.user.id):
        user = request.user
    else:
        user = GSNUser.objects.get(id=request.user.id)

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


# OAUTH WORK FLOW:
# 1- go OAUTH provider and follow the steps until redirect
# 2- Redirected to page /gsn/logged with parameter id; id is the code, we get the tokens and create a new user in the DB with the according fields
# 3-

# View that redirects to OAUTH provider




def oauth_logging_redirect(request):
    return redirect(oauth_server_address + oauth_auth_suffix, response_type='code', client_id=oauth_client_id,
                    oauth_client_secret=oauth_client_secret)

#
#
# def oauth_after_log(request):
#     code = request.GET['code']
#
#     if code is None:
#         return HttpResponseForbidden()
#
#     response = redirect("index")
#     response.set_cookie('access_token', code)
#
#     if User.objects.filter(code=code).exists():
#         return response
#
#     payload = {
#         'client_id': oauth_client_id,
#         'client_secret': oauth_client_secret,
#         'redirect_uri': '127.0.0.1:8000/gsn/',
#         'code': code,
#         'grant_type': 'authorization_code'
#     }
#
#     data = json.loads(
#         requests.get(oauth_server_address + oauth_token_suffix, params=payload).json())
#
#     now = datetime.now()
#     expire_date = datetime.now() + timedelta(seconds=data["expires_in"])
#
#     new_user = User(code=code, access_token=data['access_token'], refresh_token=['refresh_token'],
#                     token_created_date=now, token_expire_date=expire_date)
#
#     new_user.save()
#
#     return response
