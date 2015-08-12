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
    , 'gsnMonitor'
    , 'rzModule'
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
            when('/monitor', {
                templateUrl: 'partials/monitor.html',
                resolve: {
                    sensorData: [
                        'MonitorSensorsData',
                        function (MonitorSensorsData) {
                            return MonitorSensorsData.getSensors();
                        }
                    ]
                },
                controller: 'MonitorController'
            }).
            when('/about', {
                templateUrl: 'partials/about.html'
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

gsnWebApp.controller('TabsCtrl', ['$scope', '$location', 'GsnTabs',
    function ($scope, $location, GsnTabs) {

        $scope.tabs = GsnTabs;
        $scope.tabs.updateSelectedTab($location);
        //var tabNames = ['/map', '/plot', '/monitor', '/about'];
        //$scope.tabs = [
        //    {link: '#/map', label: 'Sensor map'},
        //    {link: '#/plot', label: 'Plot data'},
        //    {link: '#/monitor', label: 'Monitor'},
        //    {link: '#/about', label: 'About'},
        //];
        //
        //if (tabNames.indexOf($location.$$path) > -1) {
        //    $scope.selectedTab = $scope.tabs[tabNames.indexOf($location.$$path)];
        //} else {
        //    $scope.selectedTab = $scope.tabs[0];
        //}
        //
        //$scope.setSelectedTab = function (tab) {
        //    console.log(tab);
        //    $scope.selectedTab = tab;
        //};
        //
        //$scope.tabClass = function (tab) {
        //    if ($scope.selectedTab == tab)
        //        return 'active';
        //    else
        //        return '';
        //
        //}
    }]);


gsnWebApp.factory('_', ['$window', function ($window) {
    return $window._; // assumes underscore has already been loaded on the page
}]);

gsnWebApp.service('GsnTabs', function () {

    function GsnTabs() {
        this.tabs = [
            {link: '#/map', label: 'Sensor map'},
            {link: '#/plot', label: 'Plot data'},
            {link: '#/monitor', label: 'Monitor'},
            {link: '#/about', label: 'About'},
        ];

        this.tabNames = ['/map', '/plot', '/monitor', '/about'];

        this.selectedTab = this.tabs[0];
    }

    GsnTabs.prototype = {
        updateSelectedTab: function (location) {
            if (this.tabNames.indexOf(location.$$path) > -1) {
                this.selectedTab = this.tabs[this.tabNames.indexOf(location.$$path)];
            } else {
                this.selectedTab = this.tabs[0];
            }
        },

        setSelectedTab: function (tab) {
            console.log(tab);
            this.selectedTab = tab;
        },
        tabClass: function (tab) {
            if (this.selectedTab == tab)
                return 'active';
            else
                return '';

        }
    };

    return new GsnTabs();

});