from django.conf.urls import url, include
from . import views
from django.views.decorators.csrf import csrf_exempt
from django.contrib import admin

urlpatterns = [
    url(r'^$', views.index, name='index'),
    url(r'^sensors/$', views.sensors, name='sensors'),
    url(r'^sensors/(?P<sensor_name>(\w)+)/(?P<from_date>(\w|:|-)+)/(?P<to_date>(\w|:|-)+)/$', views.sensor_detail,
        name='sensor_detail'),
    url(r'^download/(?P<sensor_name>(\w)+)/(?P<from_date>(\w|:|-)+)/(?P<to_date>(\w|:|-)+)/$', views.download_csv,
        name='download_csv'),
    url(r'^download/$', csrf_exempt(views.download), name='download'),
    url(r'^profile/$', views.profile, name='profile'),
    url(r'^logout/$', views.logout_view, name='logout'),
    url(r'^admin/', include(admin.site.urls)),
    url(r'^oauth_code/$', views.oauth_get_code, name='oauth_logging_redirect'),
    url(r'^favorites/$', views.favorites_manage, name='favorites'),
    url(r'^favorites_list/$', views.favorites_list, name='favorites_list'),
    url(r'^dashboard/(?P<sensor_name>(\w)+)/$', views.dashboard, name='dashboard'),

    # url(r'^logged/$', views.oauth_after_log, name='oauth_after_log'),
    url(r'^accounts/', include('allaccess.urls')),
]
