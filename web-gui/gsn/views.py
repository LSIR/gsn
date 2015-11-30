import csv
import json
from datetime import datetime, timedelta
import requests
import requests_cache
from django.conf import settings
from django.contrib.auth import authenticate, login, logout
from django.contrib.auth.decorators import login_required
from django.http import HttpResponse, JsonResponse, HttpResponseRedirect, HttpResponseNotFound
from django.http import HttpResponseForbidden
from django.shortcuts import redirect, render
from django.template import loader
from django.template.context_processors import csrf
from django.utils import timezone
from django.views.decorators.csrf import csrf_exempt
from gsn.forms import GSNUserCreationForm, ProfileForm, LoginForm

# Server adress and services


# OAUTH2
oauth_server_address = settings.GSN['SERVER_URL']
oauth_client_id = settings.GSN['CLIENT_ID']
oauth_client_secret = settings.GSN['CLIENT_SECRET']
oauth_redirection_url = settings.GSN['REDIRECTION_URL']
oauth_sensors_url = settings.GSN['SENSORS_URL']
oauth_auth_url = settings.GSN['AUTH_URL']
oauth_token_url = settings.GSN['TOKEN_URL']
max_query_size = settings.GSN['MAX_QUERY_SIZE']

# Cache setup
requests_cache.install_cache("demo_cache")


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
            'logged_out': 'true', }

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
def dashboard(request):
    if request.user.gsnuser.favorites is None:
        return HttpResponseNotFound()

    data = {}

    payload = {
        'latestValues': True
    }

    for sensor_name in request.user.gsnuser.favorites:
        r = requests.get(oauth_sensors_url + '/' + sensor_name, params=payload,
                         headers=create_headers(request.user.gsnuser.favorites))
        sensor_data = json.loads(r.text)

        data[sensor_name] = {
            'values': sensor_data['properties']['values'],
            'geographical': sensor_data['properties']['geographical'],
            'fields': sensor_data['properties']['field']
        }

    return JsonResponse(data)

    pass


@login_required
def favorite_manage(request):
    if request.method == "GET":
        if 'add' in request.POST:
            request.user.gsnuser.favorites.extend(request.GET.get('add'))
            request.user.gsnuser.save()
            return HttpResponse('')
        elif 'remove' in request.POST:
            try:
                request.user.gsnuser.favorites.remove(request.GET.get('remove'))
                request.user.gsnuser.save()
            except ValueError:
                pass
            return HttpResponse('')

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
            'has_access': True
        }

        payload = {
            'from': from_date,
            'to': to_date,
            'size': max_query_size
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


def login_request(request):
    """
    Log in the user from a LoginForm, assuming the credentials are correct. If they are, the user will be redirected to
    the index page

    If the credentials are incorrect or the user is inactive or the form is invalid, the user will be redirected to the
    /gsn/login/ page, with the appropriate error message.

    If the manages to log in successfuly, the function checks if there is a 'next' parameter; if there is, the user is
    redirected to that page. If not, he is redirected to /gsn/
    """

    context = {
        'next': request.GET.get('next')
    }
    context.update(csrf(request))

    if request.method == "POST":

        form = LoginForm(data=request.POST)
        context.update({
            'form': form
        })

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
                """
                else:
                    context.update({
                        'error_message': 'This user has been desactivated'
                    })
            else:
                context.update({
                    'error_message': 'This username and password combination is not valid'

                })
        else:
            context.update({
                'error_message': 'You didn\'t fill the form correctly'
            })
            """
    else:
        context.update({
            'form': LoginForm(request)
        })
        pass

    return render(request, 'gsn/login.html', context)


def sign_up(request):
    """
    View that lets a user sign up for the website.

    Ask for username, email and password using a GSNUserCreationForm. If the form is valid, the user is created, then
    the login page is rendered and the user is prompted to log in.

    If a user was already logged in, he will be logged out before this view is rendered.
    """

    # TODO: Find a way to integrate more meaningful error messages

    if request.user.is_authenticated():
        logout(request)

    context = {}

    context.update(csrf(request))

    if request.method == "POST":

        form = GSNUserCreationForm(request.POST)

        if form.is_valid():
            form.save()

            context.update({
                'green_message': 'Your account was successfuly created. Please proceed to login.',
                'form': LoginForm()
            })

            return render(request, 'gsn/login.html', context)

            # login(request)

        else:

            context.update({
                'error_message': 'A problem happened while submitting this form. It may be that this '
                                 'username is already taken, that the passwords don\'t match or that '
                                 'the '
                                 'constraint on '
                                 'the fields are not respected.'
            })

    else:
        form = GSNUserCreationForm()

    context.update({
        'form': form
    })

    return render(request, 'gsn/sign_up.html', context)


def logout_view(request):
    """
    Logs out the user then redirected him to the /gsn/ page
    """
    logout(request)
    return redirect('index')


@login_required
def profile(request):
    """
    Profile of the user; contains a ProfileForm that lets the user edit their email adress, force refresh the oauth2
    tokens or link their account to the server

    If the user isn't a logged in or a GSNUser, he will be redirected to the login page, after which he will be
    redirected there on successful login

    If the user was redirected here from the oauth server, this view catches the code and generates the according oauth
    tokens
    """
    context = {
        'username': request.user.username
    }

    context.update(csrf(request))

    user = request.user

    prepopulate = {
        'username': user.username,
        'email': user.email,
        'access_token': user.gsnuser.access_token,
        'refresh_token': user.gsnuser.refresh_token,
        'token_expire_date': user.gsnuser.token_expire_date
    }

    code = request.GET.get('code')
    if code is not None:
        create_token(code, user.gsnuser)
        return redirect('profile')

    if request.method == "POST":

        copy = request.POST.copy()
        copy.update({
            'username': user.username
        })
        form = ProfileForm(copy, instance=user.gsnuser)

        if form.is_valid():
            user.email = form.cleaned_data['email']
            user.save()
            prepopulate['email'] = user.email
            form = ProfileForm(prepopulate, instance=user.gsnuser)
            context.update({
                'success_message': 'User profile successfuly updated'
            })
        else:
            context.update({
                'errors': form.errors
            })
            form = ProfileForm(prepopulate, instance=user.gsnuser)


    else:
        form = ProfileForm(prepopulate, instance=user.gsnuser)

    context.update(dict(form=form))

    return render(request, 'gsn/profile.html', context)


def oauth_get_code(request):
    """
    Redirects to the oauth server
    """
    return HttpResponseRedirect(
        oauth_auth_url + '?' + 'response_type=code' + '&client_id=' + oauth_client_id + '&client_secret=' +
        oauth_client_secret)


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
    user.token_created_date = timezone.now()
    user.token_expire_date = user.token_created_date + timedelta(seconds=data['expires_in'])
    user.save()

    return user.access_token


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
