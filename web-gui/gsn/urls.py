from django.conf.urls import url
from . import views

urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^sensors/$', views.sensors, name='sensors'),
    url(r'^sensors/(?P<sensor_name>(\w)+)/(?P<from_date>(\w|:|-)+)/(?P<to_date>(\w|:|-)+)/$', views.sensor_detail,
        name='sensor_detail')
]
