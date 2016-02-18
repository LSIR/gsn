
'use strict';

/* App Module */

var gsnApp = angular.module('gsnApp', [
    'ngRoute',
    'gsnControllers',
    'ng.django.urls'
]);


gsnApp.config(['$routeProvider',
    function ($routeProvider) {
        $routeProvider.
        when('/sensors', {
            templateUrl: 'static/html/sensors_list.html',
            controller: 'SensorListCtrl'
        }).
        when('/sensors/:sensorName', {
            templateUrl: 'static/html/sensor-detail.html',
            controller: 'SensorDetailsCtrl'
        }).
        when('/map', {
            templateUrl: 'static/html/map.html',
            controller: 'MapCtrl'
        }).
        when('/compare', {
            templateUrl: 'static/html/compare.html',
            controller: 'CompareCtrl'
        }).
        when('/download', {
            templateUrl: 'static/html/download.html',
            controller: 'DownloadCtrl'
        }).
        when('/dashboard', {
            templateUrl: 'static/html/dashboard.html',
            controller: 'DashboardCtrl'
        }).

        otherwise({
            redirectTo: '/sensors'
        });
    }]);
