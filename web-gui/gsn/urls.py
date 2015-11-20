from django.conf.urls import url
from . import views
from django.views.decorators.csrf import csrf_exempt

urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^sensors/$', views.sensors, name='sensors'),
    url(r'^sensors/(?P<sensor_name>(\w)+)/(?P<from_date>(\w|:|-)+)/(?P<to_date>(\w|:|-)+)/$', views.sensor_detail,
        name='sensor_detail'),
    url(r'^download/(?P<sensor_name>(\w)+)/(?P<from_date>(\w|:|-)+)/(?P<to_date>(\w|:|-)+)/$', views.download_csv,
        name='download_csv'),
    url(r'^download/$', csrf_exempt(views.download),
        name='download')

]
