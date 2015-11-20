import json
from datetime import datetime
from django.views.generic import TemplateView
from django.http import HttpResponse, JsonResponse
from django.template import loader
import requests
import requests_cache
from django.views.decorators.csrf import csrf_exempt
from django.http import HttpResponseForbidden
from gsn.forms import TestForm
import csv

_from_ = '2015-10-06T13:04:04'
_to_ = '2015-10-06T13:06:04'

server_address = "http://montblanc.slf.ch:22001/rest/sensors"
# server_address = "http://opensense.epfl.ch:22001/rest/sensors"

# Create your views here.

requests_cache.install_cache("demo_cache")


def index(request):
    template = loader.get_template('gsn/index.html')

    return HttpResponse(template.render())


def sensors(request):
    return JsonResponse(json.loads(requests.get(server_address).text))


def sensor_detail(request, sensor_name, from_date, to_date):
    # _from_ = str(datetime.now().replace(microsecond=0).isoformat(sep='T'))
    # _to_ = _from_

    payload = {
        'from': from_date,
        'to': to_date,
        'username': 'john',
        'password': 'john'
    }

    return JsonResponse(json.loads(requests.get(server_address + '/' + sensor_name + '/', params=payload).text))


def download_csv(request, sensor_name, from_date, to_date):
    payload = {
        'from': from_date,
        'to': to_date,
        'username': 'john',
        'password': 'john'
    }

    data = json.loads(requests.get(server_address + '/' + sensor_name + '/', params=payload).text)

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
    writer.writerow([field['name'] + " (" + field['unit'] + " " + field['type'] + ")"
                     for field in data['properties']['fields']])

    for value in data['properties']['values']:
        writer.writerow(value)

    return response
