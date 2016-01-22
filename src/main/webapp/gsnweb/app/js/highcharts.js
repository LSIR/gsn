'use strict';

angular.module('hcControllers', [])


    .controller('hcCtrl', ['$scope', 'dataProcessingService',
        'ChartConfigService', 'FilterParameters', 'ProcessGsnData',
        function ($scope, dataProcessingService,
                  ChartConfigService, FilterParameters, ProcessGsnData) {

            dataProcessingService.resetPromise();

            FilterParameters.getSensorModels().then(function () {
                $scope.canPlot = FilterParameters.hasRequiredParameters();

                console.log("SENSOR MODELS" + FilterParameters.sensorModels);


                if ($scope.canPlot) {
                    updatePlotModel();
                }

            });


            $scope.$on('handleBroadcast', function () {

                dataProcessingService.resetPromise();

                updatePlotModel();
            });

            function updatePlotModel() {
                $scope.pointCount = 0;
                $scope.dataProcessing = true;
                $scope.dataLoading = true;

                $scope.axisInfo = FilterParameters.getAllSelectedParameters();
                var dataLoadingPromise = dataProcessingService.loadData().then(function (d) {

                    if (d.status === 'OK') {
                        $scope.pointCount = d.data.split(/\r\n|\n/).length;
                        $scope.csvData = d.data;

                        if ($scope.pointCount * FilterParameters.getHeaders().length / FilterParameters.sensorModels.length > 40000) {
                            throw $scope.pointCount;
                        }
                        $scope.dataLoading = false;
                    }

                })
                    .finally(function () {
                        $scope.dataLoading = false;
                    });

                dataLoadingPromise.then(function () {

                    var processed = {};
                    if (FilterParameters.sensorModels.length == 1) {
                        processed = ProcessGsnData.process($scope.csvData);
                    } else {
                        processed = ProcessGsnData.processMultiSensors($scope.csvData);
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
                    if (!isNaN(error)) {
                        $scope.errorTooMany = error;
                    } else {
                        if(error.status === 406) {
                            $scope.errorAccessFailed = 'Selected sensor is protected! If you believe you should have access to this sensor please login or contact us.'
                        } else {
                            $scope.errorAccessFailed = error.statusText;
                        }
                    }
                    $scope.noData = true;
                }).finally(function () {
                    $scope.dataProcessing = false;
                });
            }

        }]);





