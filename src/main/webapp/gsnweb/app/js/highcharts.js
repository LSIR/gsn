'use strict';

angular.module('hcControllers', [])


    .controller('hcCtrl', ['$scope', 'AxisInfo', 'dataProcessingService',
        'ChartConfigService', 'FilterParameters', 'ProcessGsnData', '$window',
        function ($scope, AxisInfo, dataProcessingService,
                  ChartConfigService, FilterParameters, ProcessGsnData, $window) {


            $scope.canPlot = FilterParameters.hasRequiredParameters();

            if ($scope.canPlot) {
                AxisInfo.getAxesInfo().then(function (d) {

                    updatePlotModel(d);
                });
            }


            $scope.$on('handleBroadcast', function () {

                AxisInfo.resetPromise();

                dataProcessingService.resetPromise();

                AxisInfo.getAxesInfo(true).then(function (d) {

                    updatePlotModel(d);
                });
            });

            function updatePlotModel(d) {
                $scope.pointCount = 0;
                $scope.dataProcessing = true;
                $scope.dataLoading = true;
                $scope.axisInfo = d;
                var dataLoadingPromise = dataProcessingService.async().then(function (d) {

                    $scope.pointCount = d.split(/\r\n|\n/).length;
                   $scope.csvData = d;
                    //$window.alert("Loaded data " + $scope.pointCount);

                    if ($scope.pointCount*FilterParameters.getFields().length > 20000) {
                        throw $scope.pointCount;
                    }
                    $scope.dataLoading = false;

                })
                    .finally(function () {
                        $scope.dataLoading = false;
                    });

                dataLoadingPromise.then (function () {



                    var processed = ProcessGsnData.process($scope.csvData);
                    $scope.dataMap = processed.dataMap;
                    $scope.chartConfig = ChartConfigService.buildChartConfig(processed, $scope.axisInfo, FilterParameters.vs);
                    $scope.missingData = Object.keys(processed.missingData);
                    $scope.noData =  false;
                    if ( $scope.missingData.length === FilterParameters.getFields().length
                        || !processed.hasValues) {
                        $scope.noData =  true;
                        $scope.missingData = Object.keys(processed.dataMap);
                    }
                    $scope.pointCount = processed.pointCount;
                }, function(error) {
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


