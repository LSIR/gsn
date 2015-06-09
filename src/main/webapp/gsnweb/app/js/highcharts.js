'use strict';

angular.module('hcControllers', [])


    .controller('hcCtrl', ['$scope', 'AxisInfo', 'dataProcessingService',
        'ChartConfigService', 'FilterParameters',
        function ($scope, AxisInfo, dataProcessingService,
                  ChartConfigService, FilterParameters) {



            AxisInfo.getAxesInfo().then(function (d) {

                updatePlotModel(d);
            });


            $scope.$on('handleBroadcast', function () {

                AxisInfo.resetPromise();

                dataProcessingService.resetPromise();

                AxisInfo.getAxesInfo(true).then(function (d) {

                    updatePlotModel(d);
                });
            });

            function updatePlotModel(d) {
                $scope.dataLoading = true;
                $scope.axisInfo = d;
                dataProcessingService.async().then(function (d) {
                    $scope.dataMap = d.dataMap;
                    $scope.chartConfig = ChartConfigService.buildChartConfig(d, $scope.axisInfo, FilterParameters.vs);
                    $scope.missingData = Object.keys(d.missingData);
                    $scope.noData =  false;
                    if ( $scope.missingData.length === FilterParameters.getFields().length
                    || !d.hasValues) {
                        $scope.noData =  true;
                        $scope.missingData = Object.keys(d.dataMap);
                    }
                    $scope.pointCount = d.pointCount;
                })
                    .finally(function () {
                        $scope.dataLoading = false;
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


