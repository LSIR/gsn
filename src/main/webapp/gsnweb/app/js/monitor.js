var gsnMonitor = angular.module("gsnMonitor", []);

gsnMonitor.controller("MonitorController", ["$scope", 'sensorData',
    function ($scope, sensorData) {

        $scope.data = sensorData;

    }]);

sensorData.factory('MonitorSensors', ['$route',
    function ($route) {


        function MonitorSensors() {
            if ($route.current.params.sensors)
                this.sensors = $route.current.params.sensors.split(',');
            else this.sensors = [];
        }

        return MonitorSensors;
    }]);

gsnMonitor.factory('MonitorSensorsData', ['$routeParams', '$http', '$q', 'MonitorSensors',
    function ($routeParams, $http, $q, MonitorSensors) {
        var sdo = {
            getSensors: function () {

                var sensorNames = new MonitorSensors().sensors;
                //var sensorNames = ['wannengrat_wan7', 'wannengrat_wan6'];

                //if (routeParams['sensors'])
                //    sensorNames = routeParams['sensors'].split(',');


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

