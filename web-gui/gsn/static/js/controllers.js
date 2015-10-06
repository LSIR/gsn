/**
 * Created by julie_000 on 24/09/2015.
 */


'use strict';

/* Controllers */

var gsnControllers = angular.module('gsnControllers', []);

gsnControllers.controller('SensorListCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.loading = true;


    $http.get('sensors').success(function (data) {
        $scope.sensors = data.features;
        $scope.loading = false;
    });

}]);


gsnControllers.controller('SensorDetailsCtrl', ['$scope', '$http', '$routeParams', function ($scope, $http, $routeParams) {
    $scope.loading = true;
    $scope.sensorName = $routeParams.sensorName;

    $http.get('sensors/' + $routeParams.sensorName).success(function (data) {
        $scope.details = data.features;
        $scope.loading = false;

    });

    //TODO

}]);