/**
 * Created by julie_000 on 24/09/2015.
 */


'use strict';

/* Controllers */

var gsnControllers = angular.module('gsnControllers',
    [
        'angularUtils.directives.dirPagination',
        'chart.js',
        'ngMap',
        'angularSpinner',
        'ngAutocomplete',
        'highcharts-ng',
        'LocalStorageModule',
        'ui.bootstrap.tabs'
    ]
);

gsnControllers.config(function (localStorageServiceProvider) {
    localStorageServiceProvider
        .setPrefix('gsn_web_gui');
});

gsnControllers.service('favoritesService', [$http, function ($http) {
    this.remove = function (sensor_name) {
        $http.get('favorite/', {
            params: {'remove': sensor_name}
        }).success(function (data, status, headers, config) {
            return true
        }).error(function (data, status, headers, config) {
            return false
        });
    };

    this.add = function (sensor_name) {
        $http.get('favorite/', {
            params: {'add': sensor_name}
        }).success(function (data, status, headers, config) {
            return true
        }).error(function (data, status, headers, config) {
            return false
        });


    }
}]);

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

gsnControllers.service('compareService', ['localStorageService', function (localStorageService) {

    this.compareSet = [];

    this.buildData = function (keys) {

        this.compareSet = [];
        var order = 0;

        for (var key in keys) {


            var value = JSON.parse(JSON.stringify(localStorageService.get(keys[key])));

            for (var elem in value) {
                value[elem]['name'] += ' ( ' + keys[key] + ' ) ';
                value[elem]['id'] = order;

                this.compareSet.push(value[elem]);


                order++;
            }


        }
        return this;
    };


    this.filter = function (elemToFilter) {


        if (substrings.some(function (v) {
                return str.indexOf(v) >= 0;
            })) {
            // There's at least one
        }


    }

}]);


gsnControllers.service('downloadService', ['$window', '$http', function ($window, $http) {

    this.download = function (scope) {
        $http.post('download/', scope.details).success(function (data, status, headers, config) {

            var myBlob = new Blob([data], {type: 'text/html'});
            var blobURL = ($window.URL || $window.webkitURL).createObjectURL(myBlob);
            var anchor = document.createElement("a");
            anchor.download = scope.sensorName + ".csv";
            anchor.href = blobURL;
            anchor.click();

        }).error(function (data, status, headers, config) {
            console.log('Post failed')
        });

    };

    this.downloadMultiple = function (sensorList, from, to) {

        sensorList.forEach(function (sensor) {

            $http.get('download/' + sensor + '/' + from + '/' + to + '/').success(function (data, status, headers, config) {
                var myBlob = new Blob([data], {type: 'text/html'});
                var blobURL = ($window.URL || $window.webkitURL).createObjectURL(myBlob);
                var anchor = document.createElement("a");
                anchor.download = sensor + ".csv";
                anchor.href = blobURL;
                anchor.click();
            }).error(function (data, status, headers, config) {
                $window.alert('You do not have access to the sensor ' + sensor)
            });

        })
    }

}]);


gsnControllers.controller('DownloadCtrl', ['$scope', '$window', '$http', 'sensorService', 'downloadService', function ($scope, $window, $http, sensorService, downloadService) {

    var today = new Date().toJSON();
    var yesterday = new Date((new Date()).getTime() - (1000 * 60 * 60)).toJSON();


    $scope.date = {
        from: {
            date: yesterday.slice(0, 19),
            config: {
                dropdownSelector: '#dropdown2',
                minuteStep: 1
            },
            onTimeSet: function () {
                console.log('ayy');

                if (new Date($scope.date.from.date) > new Date($scope.date.to.date)) {
                    $scope.date.to.date = $scope.date.from.date
                }

            }
        },
        to: {
            date: today.slice(0, 19),
            config: {
                dropdownSelector: '#dropdown2',
                minuteStep: 1
            },
            onTimeSet: function () {
                if (new Date($scope.date.from.date) > new Date($scope.date.to.date)) {
                    $scope.date.from.date = $scope.date.to.date;
                    console.log('lmao');
                }

            }
        }
    };

    sensorService.async().success(function (data) {


        $scope.sensorsList = [];

        if (data.user.logged) {
            data.features.forEach(function (sensor) {
                $scope.sensorsList.push(sensor['properties']['vs_name']
                )
            })
        }

    });

    $scope.download = downloadService.downloadMultiple;

    $scope.minMultipleSelectSize = 20;


}]);

gsnControllers.controller('CompareCtrl', ['$scope', 'compareService', 'localStorageService', function ($scope, compareService, localStorageService) {

    $scope.filterTermsIn = '';
    $scope.filterTermsOut = '';

    var filterIn = function (elem) {


        var substrings = $scope.filterTermsIn.split(';');
        var contains = false;

        if (substrings.some(function (v) {
                return elem['name'].indexOf(v) >= 0;
            })) {
            contains = true;
        }


        return contains;

    };

    var filterOut = function (elem) {


        if ($scope.filterTermsOut == '') {
            return true;
        }

        var substrings = $scope.filterTermsOut.split(';');
        substrings = substrings.filter(Boolean);

        var contains = true;

        if (substrings.some(function (v) {
                return elem['name'].indexOf(v) >= 0;
            })) {
            contains = false;
        }


        return contains;

    };


    $scope.update = function () {
        $scope.chartConfig.series.loading = true;

        $scope.sensorsSet = compareService.buildData(localStorageService.keys()).compareSet;
        $scope.chartConfig.series = $scope.sensorsSet.filter(filterIn).filter(filterOut);
        $scope.chartConfig.loading = false;
        $scope.sensors = localStorageService.keys();

    };

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
        useHighStocks: true,
        size: {
            height: 500
        },
        loading: true,
        yAxis: {
            labels: {
                align: 'left',
                opposite: false
            }
        }
    };

    $scope.remove = function (key) {
        localStorageService.remove(key);
        $scope.update();
    };

    $scope.update();


}]);

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

                if ($scope.sensors[i].geometry.coordinates[1] && $scope.sensors[i].geometry.coordinates[0]) {
                    var latLng = new google.maps.LatLng($scope.sensors[i].geometry.coordinates[0], $scope.sensors[i].geometry.coordinates[1]);

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

gsnControllers.controller('SensorDetailsCtrl', ['$scope', '$http', '$routeParams', '$window', 'downloadService', 'localStorageService', 'favoritesService',
    function ($scope, $http, $routeParams, $window, downloadService, localStorageService, favoritesService) {


        $scope.loading = true;
        $scope.sensorName = $routeParams.sensorName;

        $scope.truePageSize = 25;

        $scope.updateRowCount = function (pageSize) {
            $scope.truePageSize = pageSize
        };

        var today = new Date().toJSON();
        var yesterday = new Date((new Date()).getTime() - (1000 * 60 * 60)).toJSON();


        $scope.date = {
            from: {
                date: yesterday.slice(0, 19),
                config: {
                    dropdownSelector: '#dropdown2',
                    minuteStep: 1
                },
                onTimeSet: function () {
                    console.log('ayy');

                    if (new Date($scope.date.from.date) > new Date($scope.date.to.date)) {
                        $scope.date.to.date = $scope.date.from.date
                    }

                }
            },
            to: {
                date: today.slice(0, 19),
                config: {
                    dropdownSelector: '#dropdown2',
                    minuteStep: 1
                },
                onTimeSet: function () {
                    if (new Date($scope.date.from.date) > new Date($scope.date.to.date)) {
                        $scope.date.from.date = $scope.date.to.date;
                        console.log('lmao');
                    }

                }
            }
        };


        //function toISO8601String(date) {
        //    return date.year + "-" + date.month + "-" + date.day + "T" + date.hour + ":" + date.minute + ":" + date.second;
        //}


        $scope.load = function () {
            $http.get('sensors/' + $routeParams.sensorName + '/' + $scope.date.from.date + '/' + $scope.date.to.date + '/').success(function (data) {
                $scope.details = data.properties ? data : undefined;

                $scope.loading = false;
                console.log('sensors/' + $routeParams.sensorName + '/' + $scope.date.from.date + '/' + $scope.date.to.date + '/');    //TODO: REMOVE


                buildData();

            });
        };

        $scope.columns = [true, true, true];

        $scope.submit = function () {
            $scope.load();
        };

        function buildData() {

            if ($scope.details && $scope.details.properties.values) {
                var k, offset = 0;

                $scope.chartConfig.series = [];

                for (k = 2; k < $scope.details.properties.fields.length; k++) {


                    $scope.chartConfig.series.push({
                        name: $scope.details.properties.fields[k].name + " (" + (!($scope.details.properties.fields[k].unit === null) ? $scope.details.properties.fields[k].unit : "no unit") + ") ",
                        id: k,
                        data: []
                    });

                    var i;
                    for (i = 0; i < $scope.details.properties.values.length; i++) {

                        if (typeof $scope.details.properties.values[i][k] === 'string' || $scope.details.properties.values[i][k] instanceof String) {
                            offset++;
                            break
                        }
                        var array = [$scope.details.properties.values[i][1], $scope.details.properties.values[i][k]];
                        $scope.chartConfig.series[k - 2].data.push(array)

                    }


                    $scope.chartConfig.series[k - 2].data.sort(function (a, b) {
                        return a[0] - b[0]
                    });

                }

                $scope.chartConfig.series = $scope.chartConfig.series.filter(function (serie) {
                    return serie.data.length > 0
                });


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
            useHighStocks: true,
            size: {
                height: 500
            },
            yAxis: {
                labels: {
                    align: 'left'
                }
            }
        };


        $scope.compare = function () {
            localStorageService.set($scope.sensorName, $scope.chartConfig.series);
            $scope.series = localStorageService.get($scope.sensorName);
        };

        //$scope.downloadCsv = function () {
        //    $window.open('download/' + $routeParams.sensorName + '/' + $scope.date.from.date + '/' + $scope.date.to.date + '/')
        //};

        $scope.download = function () {
            downloadService.download($scope);
        };


        $scope.load();

        $scope.addFavorite = function (sensor_name) {
            if (favoritesService.add(sensor_name)) {
                $scope.favorite = True
            } else {
                console.log('Failed to add to favorites !')
            }
        };

        $scope.remove = function (sensor_name) {
            if (favoritesService.remove(sensor_name)) {
                $scope.favorite = False
            } else {
                console.log('Failed to remove from favorites !')
            }
        }


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


gsnControllers.controller('DashboardCtrl', ['$scope', '$http', '$interval', 'favoritesService', function ($scope, $http, $interval, favoritesService) {

    $scope.refresh_interval = 60000;

    $scope.load = function () {
        $http.get('dashboard/').success(function (data, status, headers, config) {
            $scope.sensors = data
        }).error(function (data, status, headers, config) {
            if (status === 404) {
                $scope.error_message = "You do not have any favorite sensors set !"
            } else {
                $scope.error_message = "Something went terribly wrong with the server. Please try again later."
            }
        });
    };

    $scope.remove = function (sensor_name) {

        if (favoritesService.remove(sensor_name)) {
            $scope.success_message = 'Sensor ' + sensor_name + ' successfuly removed from favorites';
            $scope.load()
        } else {
            $scope.error_message = "Something went terribly wrong with the server. Please try again later."
        }
    };

    var interv = $interval(load, $scope.refresh_interval);
    $scope.$on('$destroy', function () {
        $interval.cancel(interv);
    });


}]);


