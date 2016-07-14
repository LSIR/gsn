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
        'ui.bootstrap',
        'ui.bootstrap.datetimepicker',
        'ui.dateTimeInput',
        'ngWebSocket'
    ]
);

gsnControllers.factory('SensorDataStream', function($websocket) {

    var methods = {
        register: function(vsname, stream_callback){
            var dataStream = $websocket(WEBSOCKET_URL + 'api/sensors/'+vsname+'/stream');
            dataStream.onMessage(function(message) {
                stream_callback(JSON.parse(message.data));
            });
            //scope.stream_send = function() { dataStream.send(JSON.stringify({ action: 'get' })); }
        }
    };

    return methods;
});

gsnControllers.config(function (localStorageServiceProvider) {
    localStorageServiceProvider
        .setPrefix('gsn_web_gui');
});

gsnControllers.service('favoritesService', function ($http) {
    this.remove = function (sensor_name) {
        return $http.get('favorites/', {
            params: {'remove': sensor_name}
        })
    };

    this.add = function (sensor_name) {
        return $http.get('favorites/', {
            params: {'add': sensor_name}
        })
    };

    this.list = function () {
        return $http.get("favorites_list/")
    }

});

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
            var latLngA = new google.maps.LatLng(circlePosition.split(",")[1], circlePosition.split(",")[0]);


            if (!sensor.geometry.coordinates.some(function (v) {
                    return v != null;
                })
            ) {
                return null
            }

            var latLngB = new google.maps.LatLng(sensor.geometry.coordinates[0], sensor.geometry.coordinates[1]);


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

        //if (data.user.logged) {
        data.features.forEach(function (sensor) {
            $scope.sensorsList.push(sensor['properties']['vs_name']
            )
        });
        //}

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

gsnControllers.controller('SensorListCtrl', ['$scope', 'sensorService', 'favoritesService', function ($scope, sensorService, favoritesService) {

    $scope.loading = true;

    var map;


    sensorService.async().success(function (data) {
        $scope.sensors = data.features;
        $scope.loading = false;

        favoritesService.list().success(function (data, status, headers, config) {
            $scope.favorites = data.favorites_list;

        }).error(function (data, status, headers, config) {
        });

        $scope.$on('mapInitialized', function (event, evtMap) {


            map = evtMap;

            $scope.dynMarkers = [];

            for (var i = 0; i < $scope.sensors.length; i++) {

                if ($scope.sensors[i].properties.vs_name == "p_osgps"){
                    for(var j = 0; j < $scope.sensors[i].properties.values.length; j++){
                        var latLng = new google.maps.LatLng($scope.sensors[i].properties.values[j][2], $scope.sensors[i].properties.values[j][3]);

                        var marker = new google.maps.Marker({
                            position: latLng,
                            title: $scope.sensors[i].properties.vs_name + "[" + $scope.sensors[i].properties.values[j][1] + "]",
                            url: '#/sensors/' + $scope.sensors[i].properties.vs_name
                        });

                        google.maps.event.addListener(marker, 'click', function () {
                            window.location.href = this.url;
                        });

                        $scope.dynMarkers.push(marker);

                    }
                    continue;
                }
                if ($scope.sensors[i].geometry && $scope.sensors[i].geometry.coordinates[1] && $scope.sensors[i].geometry.coordinates[0]) {
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
                    }

                }
            }
        };


        //function toISO8601String(date) {
        //    return date.year + "-" + date.month + "-" + date.day + "T" + date.hour + ":" + date.minute + ":" + date.second;
        //}


        $scope.load = function () {


            $http.get('sensors/' + $routeParams.sensorName + '/' + new Date($scope.date.from.date).toJSON().slice(0, 19) + '/' + new Date($scope.date.to.date).toJSON().slice(0, 19) + '/').success(function (data) {
                $scope.details = data.properties ? data : undefined;

                $scope.loading = false;

                buildData($scope.details);

            });
        };

        $scope.columns = [true, false, true];

        $scope.submit = function () {
            $scope.load();
        };

        function buildData(details) {

            if (details && details.properties.values) {
                var k, offset = 0;

                $scope.chartConfig.series = [];

                for (k = 2; k < details.properties.fields.length; k++) {


                    $scope.chartConfig.series.push({
                        name: details.properties.fields[k].name + " (" + (!(details.properties.fields[k].unit === null) ? details.properties.fields[k].unit : "no unit") + ") ",
                        id: k,
                        data: []
                    });

                    var i;
                    for (i = 0; i < details.properties.values.length; i++) {

                        if (typeof details.properties.values[i][k] === 'string' || details.properties.values[i][k] instanceof String) {
                            offset++;
                            break
                        }
                        var array = [details.properties.values[i][1], details.properties.values[i][k]];
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

            favoritesService.add(sensor_name).success(function (data, status, headers, config) {
                $scope.load()
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.removeFavorite = function (sensor_name) {

            favoritesService.remove(sensor_name).success(function (data, status, headers, config) {
                $scope.load()
            }).error(function (data, status, headers, config) {
            });
        };

        $scope.filterFunctionList = [];

        $scope.filterValuesList = [];

        $scope.filterOperators = ['==', '!=', '>=', '>', '<=', '<'];


        // Adds to the filter list a filter function
        $scope.addFilter = function (ind, op, value, index) {
            function filterFunc(ind, op, value) {
                return function (a) {


                    try {
                        return eval(a[ind] + op + value)
                    } catch (e) {
                        return false
                    }

                }
            }


            $scope.filterFunctionList.splice(index, 1, filterFunc(ind, op, value))

        };


        $scope.filter = function () {
            var dataset = JSON.parse(JSON.stringify($scope.details.properties.values));

            for (var j = 0; j < $scope.filterFunctionList.length; j++) {
                dataset = dataset.filter($scope.filterFunctionList[j])
            }
            var c = JSON.parse(JSON.stringify($scope.details));
            c.properties.values = dataset;

            console.log(dataset);

            buildData(c)
        };

        $scope.removeFilter = function (index) {
            $scope.filterFunctionList.splice(index, 1);
            $scope.filterValuesList.splice(index, 1)
        };

        $scope.applyFilterChanges = function () {

            for (var i = 0; i < $scope.filterFunctionList.length; i++) {
                if ($scope.filterFunctionList[i] && $scope.filterValuesList[i] && $scope.filterValuesList[i][0] && $scope.filterValuesList[i][1] && $scope.filterValuesList[i][2]) {

                    $scope.addFilter($scope.details.properties.fields.indexOf($scope.filterValuesList[i][0]),
                        $scope.filterValuesList[i][1],
                        $scope.filterValuesList[i][2], i)

                }
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

            if (!sensor.geometry) {
                return false;
            }

            var dist = mapDistanceService.distance($scope.circlePosition, sensor);

            return (dist < $scope.radius) && dist != null;
        }
    };


}])
;


gsnControllers.controller('DashboardCtrl', ['$scope', '$http', '$interval', 'favoritesService', 'SensorDataStream', function ($scope, $http, $interval, favoritesService, SensorDataStream) {

    $scope.refresh_interval = 60000;
    $scope.sensors = {};
    $scope.SensorDataStream = SensorDataStream;

    $scope.load = function () {


        favoritesService.list().success(function (data, status, headers, config) {

            data.favorites_list.forEach(function (sensor_name) {
                $http.get('dashboard/' + sensor_name).success(function (data, status, headers, config) {
                    $scope.sensors[sensor_name] = data
                    $scope.SensorDataStream.register(sensor_name.toLowerCase(), function (data){
                        if (data) {
                            for (var k = 0; k < $scope.sensors[sensor_name].fields.length; k++) {
                                    $scope.sensors[sensor_name].values[k] = data[$scope.sensors[sensor_name].fields[k].name.toLowerCase()];
                                }
                            }
                        });

                }).error(function (data, status, headers, config) {
                    $scope.error_message = "Something went wrong when getting the data of the sensor " + sensor_name
                });
            })

        }).error(function (data, status, headers, config) {

            if (status == 404) {
                $scope.error_message = "You do not have any favorites set !"

            } else {
                $scope.error_message = "Something went wrong while fetching your favorites "
            }

        });


    };

    $scope.remove = function (sensor_name) {

        console.log(favoritesService.remove(sensor_name));

        favoritesService.remove(sensor_name).success(function (data, status, headers, config) {
            $scope.success_message = 'Sensor ' + sensor_name + ' successfuly removed from favorites';
            $scope.sensors[sensor_name] = undefined;
            $scope.load()
        }).error(function (data, status, headers, config) {
            console.log(':(');
            $scope.error_message = "Something went terribly wrong with the server. Please try again later."
        });
    };

    //var interv = $interval($scope.load, $scope.refresh_interval);
    //$scope.$on('$destroy', function () {
    //    console.log($scope.sensors);
    //    $interval.cancel(interv);
    //});

    $scope.load()


}]);


