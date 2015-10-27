/**
 * Created by julie_000 on 24/09/2015.
 */


'use strict';

/* Controllers */

var gsnControllers = angular.module('gsnControllers', ['angularUtils.directives.dirPagination', 'chart.js', 'ngMap', 'angularSpinner', 'ngAutocomplete', 'highcharts-ng']);


gsnControllers.factory('sensorService', function ($http) {
    return {
        async: function () {
            return $http.get('sensors');
        }
    };
});

gsnControllers.factory('mapDistanceService', function () {
    return {
        distance: function (circlePosition, sensor) {
            var latLngA = new google.maps.LatLng(circlePosition.split(",")[0], circlePosition.split(",")[1]);

            var latLngB = new google.maps.LatLng(sensor.geometry.coordinates[1], sensor.geometry.coordinates[0]);

            return google.maps.geometry.spherical.computeDistanceBetween(latLngA, latLngB);

        }
    }
});

gsnControllers.controller('SensorListCtrl', ['$scope', 'sensorService', function ($scope, sensorService) {

    $scope.loading = true;

    var map;


    sensorService.async().success(function (data) {
        $scope.sensors = data.features;
        $scope.loading = false;

        $scope.$on('mapInitialized', function (event, evtMap) {


            map = evtMap;

            $scope.dynMarkers = [];

            for (var i = 0; i < $scope.sensors.length; i++) {

                if ($scope.sensors[i].geometry.coordinates[1] == $scope.sensors[i].geometry.coordinates[0] == 0) {
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

            }

            $scope.markerClusterer = new MarkerClusterer(map, $scope.dynMarkers, {});


        });
    });


    //$http.get('sensors').success(function (data) {
    //        $scope.sensors = data.features;
    //        $scope.loading = false;
    //
    //    }
    //);

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

            buildData();

        });
    };

    $scope.columns = [true, true, true];

    $scope.submit = function () {
        $scope.load();
    };

    function buildData() {

        if ($scope.details && $scope.details.properties.values) {
            var k;

            $scope.chartConfig.series = [];

            for (k = 2; k < $scope.details.properties.fields.length; k++) {


                $scope.chartConfig.series.push({
                    name: $scope.details.properties.fields[k].name + " (" + (!($scope.details.properties.fields[k].unit === "") ? $scope.details.properties.fields[k].unit : "no unit") + ") ",
                    id: k,
                    data: []
                });

                var i;
                for (i = 0; i < $scope.details.properties.values.length; i++) {

                    var array = [$scope.details.properties.values[i][1], $scope.details.properties.values[i][k]];
                    $scope.chartConfig.series[k - 2].data.push(array)

                }

                $scope.chartConfig.series[k - 2].data.sort(function (a, b) {
                    return a[0] - b[0]
                })

            }


        }
    }


    $scope.chartConfig = {
        options: {
            chart: {
                zoomType: 'x'
            },
            rangeSelector: {
                enabled: true
            },
            navigator: {
                enabled: true
            },
            legend: {
                enabled: true
            },
            plotOptions: {
                series: {
                    marker: {
                        enabled: false
                    }
                }
            }
        },
        series: [],
        title: {
            text: 'Data'
        },
        useHighStocks: true
    };


    console.log('sensors/' + $routeParams.sensorName + '/' + toISO8601String($scope.from) + '/' + toISO8601String($scope.to) + '/');    //TODO: REMOVE

    $scope.load();


}]);

gsnControllers.controller('MapCtrl', ['$scope', 'sensorService', 'mapDistanceService', function ($scope, sensorService, mapDistanceService) {

    $scope.loading = true;
    $scope.test = "TEST";


    $scope.defaultPosition = "46.520112399999995, 6.5659288";
    $scope.circlePosition = "46.520112399999995, 6.5659288";
    $scope.radius = 20000;

    $scope.zoomLevel = 6;

    $scope.centerChanged = function (event) {
        $scope.circlePosition = this.getCenter().lat() + ", " + this.getCenter().lng();
    };

    $scope.boundsChanged = function (event) {
        $scope.radius = this.getRadius();
    };

    $scope.centerOnMe = function () {
        $scope.circlePosition = "current-location";
        //$scope.zoomLevel = 12;
        //$scope.radius = 2000;
    };

    $scope.locationSearchResult = '';
    $scope.locationSearchDetails = '';

    $scope.locationSearch = function () {
        $scope.circlePosition = $scope.locationSearchDetails.geometry.location.lat() + ", " + $scope.locationSearchDetails.geometry.location.lng()
    };


    sensorService.async().success(function (data) {
        $scope.sensors = data.features;
        $scope.loading = false;
    });

    $scope.isCloseEnough = function () {
        return function (sensor) {
            return mapDistanceService.distance($scope.circlePosition, sensor) < $scope.radius;
        }
    };


}])
;