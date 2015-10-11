/**
 * Created by julie_000 on 24/09/2015.
 */


'use strict';

/* Controllers */

var gsnControllers = angular.module('gsnControllers', ['angularUtils.directives.dirPagination']);

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

    $scope.truePageSize = 25;

    $scope.updateRowCount = function (pageSize) {
        $scope.truePageSize = pageSize
    };

    var today = new Date().toJSON();
    var yesterday = new Date((new Date()).getTime() - (1000 * 60 * 60)).toJSON();

    $scope.to = {
        'year': parseInt(today.slice(0, 4)),
        'month': parseInt(today.slice(5, 7)),
        'day': parseInt(today.slice(8, 10)),
        'hour': parseInt(today.slice(11, 13)),
        'minute': parseInt(today.slice(14, 16)),
        'second': parseInt(today.slice(17, 19))
    };


    $scope.from = {
        'year': parseInt(yesterday.slice(0, 4)),
        'month': parseInt(yesterday.slice(5, 7)),
        'day': parseInt(yesterday.slice(8, 10)),
        'hour': parseInt(yesterday.slice(11, 13)),
        'minute': parseInt(yesterday.slice(14, 16)),
        'second': parseInt(yesterday.slice(17, 19))
    };

    function toISO8601String(date) {
        return date.year + "-" + date.month + "-" + date.day + "T" + date.hour + ":" + date.minute + ":" + date.second;
    }


    $scope.load = function () {
        $http.get('sensors/' + $routeParams.sensorName + '/' + toISO8601String($scope.from) + '/' + toISO8601String($scope.to) + '/').success(function (data) {
            $scope.details = data.features;
            $scope.loading = false;


            //TODO: REMOVE
            console.log('sensors/' + $routeParams.sensorName + '/' + toISO8601String($scope.from) + '/' + toISO8601String($scope.to) + '/');

        });
    };


    $scope.submit = function () {
        $scope.load();
    };

    $scope.columns= [true, true, true];

    $scope.load();


}]);

