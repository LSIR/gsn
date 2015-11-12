/**
 * Created by julie_000 on 24/09/2015.
 */


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
                templateUrl: 'static/sensors_list.html',
                controller: 'SensorListCtrl'
            }).
            when('/sensors/:sensorName', {
                templateUrl: 'static/sensor-detail.html',
                controller: 'SensorDetailsCtrl'
            }).
            when('/map', {
                templateUrl: 'static/map.html',
                controller: 'MapCtrl'
            }).
            when('/compare', {
                templateUrl: 'static/compare.html',
                controller: 'CompareCtrl'
            }).
            otherwise({
                redirectTo: '/sensors'
            });
    }]);
