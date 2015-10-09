import json
from datetime import datetime

from django.views.generic import TemplateView

from django.http import HttpResponse, JsonResponse
from django.template import loader
import requests
import requests_cache

from gsn.forms import TestForm

_from_ = '2015-10-06T13:04:04'
_to_ = '2015-10-06T13:06:04'

server_address = "http://montblanc.slf.ch:22001/rest/sensors"
# Create your views here.

requests_cache.install_cache("demo_cache")


class TestFormView(TemplateView):
    template = 'contact.html'

    def get_context_data(self, **kwargs):
        context = super(TestFormView, self).get_context_data(**kwargs)
        context.update(contact_form=TestForm())
        return context


def index(request):
    template = loader.get_template('gsn/index.html')

    return HttpResponse(template.render())


def sensors(request):
    return JsonResponse(json.loads(requests.get(server_address).text))


def sensor_detail(request, sensor_name):
    _from_ = str(datetime.now().replace(microsecond=0).isoformat(sep='T'))
    _to_ = _from_

    payload = {
        'from': _from_,
        'to': _to_,
        'username': 'john',
        'password': 'john'
    }

    return JsonResponse(json.loads(requests.get(server_address + '/' + sensor_name + '/', params=payload).text))
