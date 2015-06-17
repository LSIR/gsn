'use strict';

angular.module('hcControllers', [])


    .controller('hcCtrl', ['$scope', 'AxisInfo', 'dataProcessingService',
        'ChartConfigService', 'FilterParameters', 'ProcessGsnData', '$window',
        function ($scope, AxisInfo, dataProcessingService,
                  ChartConfigService, FilterParameters, ProcessGsnData, $window) {

            FilterParameters.getSensorModels().then(function () {
                $scope.canPlot = FilterParameters.hasRequiredParameters();

                console.log("SENSOR MODELS" + FilterParameters.sensorModels);


                if ($scope.canPlot) {
                    //AxisInfo.getAxesInfo().then(function (d) {

                        updatePlotModel();
                    //});
                }

            });


            $scope.$on('handleBroadcast', function () {

                AxisInfo.resetPromise();

                dataProcessingService.resetPromise();

                //AxisInfo.getAxesInfo(true).then(function (axisInfo) {

                    updatePlotModel();
                //});
            });

            function updatePlotModel() {
                $scope.pointCount = 0;
                $scope.dataProcessing = true;
                $scope.dataLoading = true;

                $scope.axisInfo = FilterParameters.getAllSelectedParameters();
                var dataLoadingPromise = dataProcessingService.async().then(function (d) {

                    $scope.pointCount = d.split(/\r\n|\n/).length;
                    $scope.csvData = d;
                    //$window.alert("Loaded data " + $scope.pointCount);

                    if ($scope.pointCount * FilterParameters.getHeaders().length > 30000) {
                        throw $scope.pointCount;
                    }
                    $scope.dataLoading = false;

                })
                    .finally(function () {
                        $scope.dataLoading = false;
                    });

                dataLoadingPromise.then(function () {


                    if (FilterParameters.sensorModels.length == 1) {
                        var processed = ProcessGsnData.process($scope.csvData);
                    } else {
                        var processed = ProcessGsnData.processMultiSensors($scope.csvData);
                    }

                    $scope.dataMap = processed.dataMap;
                    $scope.chartConfig = ChartConfigService.buildChartConfig(processed, $scope.axisInfo, FilterParameters.sensors.toString());
                    $scope.missingData = Object.keys(processed.missingData);
                    $scope.noData = false;
                    if ($scope.missingData.length === FilterParameters.getHeaders().length
                        || !processed.hasValues) {
                        $scope.noData = true;
                        $scope.missingData = Object.keys(processed.dataMap);
                    }
                    $scope.pointCount = processed.pointCount;
                }, function (error) {
                    $scope.error = error;
                    $scope.noData = true;
                }).finally(function () {
                    $scope.dataProcessing = false;
                });
            }

        }])


    .factory('sharedService', ['$rootScope', function ($rootScope) {
        var sharedService = {};

        sharedService.prepForBroadcast = function () {
            this.broadcastItem();
        };

        sharedService.broadcastItem = function () {
            $rootScope.$broadcast('handleBroadcast');
        };

        return sharedService;
    }]);


