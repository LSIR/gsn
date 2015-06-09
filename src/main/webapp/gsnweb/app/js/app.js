'use strict';

/* App Module */

var gsnWebApp = angular.module('gsnWebApp', [
    'ngRoute',
    'hcControllers',
    'ui.bootstrap',
    'highcharts-ng',
    'gsnDataServices',
    'sensorData',
    'ngAnimate', 'ngSanitize'
    , 'mgcrea.ngStrap'
    , 'metaDataServices'
    , 'allSensors'
    , 'ui.select'
    , 'ngMaterial'

]);

gsnWebApp.config(['$routeProvider',  '$datepickerProvider',
    function ($routeProvider,  $datepickerProvider) {
        $routeProvider.
            when('/plot', {
                templateUrl: 'partials/highcharts.html',
                controller: 'hcCtrl'
                , reloadOnSearch: true
            })
            //.
            //when('/test', {
            //    templateUrl: 'partials/test.html'
            //
            //})
        //    .
        //    when('/sensors', {
        //        templateUrl: 'partials/sensors.html',
        //        controller: 'AllSensorsCtrl'
        //        //, reloadOnSearch: true
        //        //  }).
        //        //otherwise({
        //        //  redirectTo: '/phones'
        //    })
        ;


        angular.extend($datepickerProvider.defaults, {
            dateFormat: 'yyyy-MM-dd',
            startWeek: 1,
            dateType: 'unix'
        });
    }
])
