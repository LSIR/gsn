/**
 * Created by julie_000 on 24/09/2015.
 */


'use strict';

/* Controllers */

var gsnControllers = angular.module('gsnControllers', ['angularUtils.directives.dirPagination', 'chart.js', 'ngMap']);

gsnControllers.controller('SensorListCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.loading = true;

    var map;

    $http.get('sensors').success(function (data) {
            $scope.sensors = data.features;
            $scope.loading = false;

        }
    );

    $scope.$on('mapInitialized', function (event, evtMap) {
        map = evtMap;

        $scope.dynMarkers = [];

        for (var i = 0; i < $scope.sensors.length; i++) {
            var latLng = new google.maps.LatLng($scope.sensors[i].geometry.coordinates[1], $scope.sensors[i].geometry.coordinates[0]);

            var marker = new google.maps.Marker({
                position: latLng,
                title: $scope.sensors[i].properties.vs_name,
                url: '#/sensors/' + $scope.sensors[i].properties.vs_name
            });

            google.maps.event.addListener(marker, 'click', function () {
                window.location.href = this.url;
            });

            $scope.dynMarkers.push(marker);

        }

        $scope.markerClusterer = new MarkerClusterer(map, $scope.dynMarkers, {});
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

    function buildSeries() {
        if ($scope.details) {
            for (var i = 2; i < $scope.details.properties.fields.length; i++) {
                $scope.plot.series.push($scope.details.properties.fields[i].name);
            }
        }
    }

    function buildLabels() {

        if ($scope.details && $scope.details.properties.values) {
            for (var i = 0; i < $scope.details.properties.values.length; i++) {
                $scope.plot.labels.push($scope.details.properties.values[i][0]);
            }
        }


    }

    function buildData() {


        if ($scope.details && $scope.details.properties.values) {

            for (var k = 2; k < $scope.details.properties.fields.length; k++) {
                $scope.plot.data.push([]);
            }


            for (var i = 0; i < $scope.details.properties.values.length; i++) {
                for (var j = 2; j < $scope.details.properties.fields.length; j++) {


                    if ($scope.details.properties.values[i][j] === "" || !$scope.details.properties.values[i][j]) {
                        $scope.plot.data[j - 2].push(0);
                    } else {
                        $scope.plot.data[j - 2].push($scope.details.properties.values[i][j]);
                    }

                }
            }
        }
    }

    $scope.load = function () {
        $http.get('sensors/' + $routeParams.sensorName + '/' + toISO8601String($scope.from) + '/' + toISO8601String($scope.to) + '/').success(function (data) {
            $scope.details = data.features ? data.features[0] : undefined;
            $scope.loading = false;


            $scope.plot = {
                'labels': [],
                'series': [],
                'data': [],
                'onClick': function (points, evt) {
                    console.log(points, evt);
                }
            };

            buildSeries();
            buildLabels();
            buildData();

            //TODO: REMOVE
            console.log('sensors/' + $routeParams.sensorName + '/' + toISO8601String($scope.from) + '/' + toISO8601String($scope.to) + '/');

        });
    };


    $scope.submit = function () {
        $scope.load();
    };

    $scope.columns = [true, true, true];

    $scope.graph = false;

    $scope.load();


}]);


