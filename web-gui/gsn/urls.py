from django.conf.urls import url, include
from . import views
from django.views.decorators.csrf import csrf_exempt

urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^(?P<access_token>\w+)$', views.index, name='index'),
    url(r'^sensors/$', views.sensors, name='sensors'),
    url(r'^sensors/(?P<sensor_name>(\w)+)/(?P<from_date>(\w|:|-)+)/(?P<to_date>(\w|:|-)+)/$', views.sensor_detail,
        name='sensor_detail'),
    url(r'^download/(?P<sensor_name>(\w)+)/(?P<from_date>(\w|:|-)+)/(?P<to_date>(\w|:|-)+)/$', views.download_csv,
        name='download_csv'),
    url(r'^download/$', csrf_exempt(views.download),
        name='download'),
    url(r'^logging/$', views.oauth_logging_redirect, name='oauth_logging_redirect'),
    url(r'^logged/$', views.oauth_after_log, name='oauth_after_log'),
    url(r'^accounts/', include('allaccess.urls')),

]
