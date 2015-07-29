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
    , 'gsnMap'
,'rzModule'
]);

gsnWebApp.config(['$routeProvider', '$datepickerProvider',
    function ($routeProvider, $datepickerProvider) {
        $routeProvider.
            when('/map', {
                templateUrl: 'partials/sensors.html',
                resolve: {
                    sensors: [
                        'Sensors',
                        function (Sensors) {
                            return Sensors.getSensors();
                        }
                    ]
                },
                controller: 'GoogleMapsController'
            }).
            when('/plot', {
                templateUrl: 'partials/highcharts.html'
                //, controller: 'hcCtrl'
                //, reloadOnSearch: true
            }).
            otherwise({redirectTo: '/map'});

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
]);

gsnWebApp.controller('TabsCtrl', function ($scope, $location) {
    $scope.tabs = [
        {link: '#/map', label: 'Sensor map'},
        {link: '#/plot', label: 'Plot data'},
    ];

    $scope.selectedTab = $scope.tabs[0];
    $scope.setSelectedTab = function (tab) {
        $scope.selectedTab = tab;
    };

    $scope.tabClass = function (tab) {
        //if ($scope.selectedTab == tab)
        //    return 'active';
        //else
        return '';

    }
});

gsnWebApp.controller('JobsCtrl', function ($scope) {

});
gsnWebApp.controller('InvoicesCtrl', function ($scope) {

});
gsnWebApp.controller('PaymentsCtrl', function ($scope) {

});


gsnWebApp.factory('_', ['$window', function($window) {
    return $window._; // assumes underscore has already been loaded on the page
}]);

