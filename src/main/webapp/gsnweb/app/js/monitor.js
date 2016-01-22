var gsnMonitor = angular.module("gsnMonitor", []);

gsnMonitor.controller("MonitorController", ["$scope", 'sensorData', 'sensorNames', '$location', 'MonitorSensors',
    function ($scope, sensorData, sensorNames, $location, MonitorSensors) {

        $scope.data = sensorData;
        $scope.sensorNames = sensorNames;

        $scope.isImage = function (sensor) {
            return JSON.stringify(sensor).indexOf('binary:image/jpg') > -1;
        };

        $scope.imageSrc = function (sensor) {
            return 'http://montblanc.slf.ch:22002/ws/api/sensors/'+
                sensor.properties['vs_name']+'/field/image';
        };

        $scope.sensors = {};
        $scope.sensors.selected = new MonitorSensors().sensors;

        $scope.update = function() {
            console.log('UPDATE');
            $location.path('/monitor');
            //var names = [];
            //for (var i = 0; i < $scope.sensors.selected.length; i++) {
            //    names.push($scope.sensors.selected[i].name);
            //}
            $location.search('sensors',  $scope.sensors.selected.toString());

        };

    }]);

gsnMonitor.factory('MonitorSensors', ['$route',
    function ($route) {


        function MonitorSensors() {
            if ($route.current.params.sensors)
                this.sensors = $route.current.params.sensors.split(',');
            else this.sensors = [];
        }

        return MonitorSensors;
    }]);

gsnMonitor.directive('lazyLoad', function ($timeout) {
    return {
        restrict: 'A',
        scope: {},
        link: function (scope, elem, attrs) {
            $timeout(function () {
                elem.attr('src', attrs.llSrc)
            });
        }
    }
});

gsnMonitor.factory('MonitorSensorsData', ['$http', '$q', 'MonitorSensors',
    function ($http, $q, MonitorSensors) {
        var sdo = {
            getSensors: function () {

                var sensorNames = new MonitorSensors().sensors;


                var promises = [];

                for (var s = 0; s < sensorNames.length; s++) {

                    var sensorName = sensorNames[s];

                    if (sensorName && sensorName.length > 0) {

                        var url = 'http://montblanc.slf.ch:22002/ws/api/sensors/' + sensorName + '?latestValues=true';

                        var promise = $http.get(url).then(function (response) {

                            return response.data;
                        }, function (reason) {
                            console.log('ERROR : ' + reason.data);
                        })

                        promises.push(promise);
                    }

                }

                return $q.all(promises).then(function (data) {
                    return data;

                });

            }
        };
        return sdo;
    }]);

