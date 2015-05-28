'use strict';

var sensorData = angular.module('sensorData', []);

sensorData.filter('propsFilter', function () {
    return function (items, props) {
        var out = [];

        if (angular.isArray(items)) {
            items.forEach(function (item) {
                var itemMatches = false;

                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                    var prop = keys[i];
                    var text = props[prop].toLowerCase();
                    if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                        itemMatches = true;
                        break;
                    }
                }

                if (itemMatches) {
                    out.push(item);
                }
            });
        } else {
            // Let the output be the input untouched
            out = items;
        }

        return out;
    };
});


sensorData.controller('ParameterSelectCtrl',
    ['$scope', 'MetadataLoader', '$location', 'sharedService', 'FilterParameters', 'AllSensors', 'SensorMetadata',
        function ($scope, MetadataLoader, $location, sharedService, FilterParameters, AllSensors, SensorMetadata) {


            $scope.nameGroupFn = function (item) {
                return item.name;
            };

            $scope.propWithUnit = function (item) {
                return item.columnName + "(" + item.unit + ")";
            };

            function updateSensorInfo(d) {
                $scope.metadata = new SensorMetadata(d);
                $scope.fields = $scope.metadata.getProperties();
                $scope.from = $scope.metadata.getFromDate();
                $scope.until = $scope.metadata.getToDate();
                $scope.multipleDemo = {};
                $scope.multipleDemo.selectedFields = [];
            }

            $scope.updateSensor = function (item) {
                FilterParameters.vs = item;

                MetadataLoader.loadData(item, true).then(function (d) {
                    //$scope.metadata = d;

                    updateSensorInfo(d);


                }).finally(function () {
                    $scope.loading = false;
                });
            };

            $scope.aggregationFunctions = [
                {value: '-1', name: ' No Aggregation'},
                {value: 'avg', name: 'AVG'},
                {value: 'max', name: 'MAX'},
                {value: 'min', name: 'MIN'},
                {value: 'sum', name: 'SUM'}
            ];

            $scope.aggFunc = {};
            $scope.aggFunc.selected = $scope.aggregationFunctions[0];

            $scope.aggregationPeriod = 1;

            $scope.aggregationUnits = [
                {value: '3600000', name: 'Hours'},
                {value: '60000', name: 'Minutes'},
                {value: '1000', name: 'Seconds'}
            ];

            $scope.aggUnit = {};
            $scope.aggUnit.selected = $scope.aggregationUnits[0];

            $scope.selectedSensor = FilterParameters.vs;

            $scope.loading = true;

            AllSensors.loadData().then(function (d) {

                $scope.sensorNames = d;


                MetadataLoader.loadData(FilterParameters.vs).then(function (d) {
                    //$scope.metadata = d;

                    //$scope.fields = $scope.metadata.features[0].properties['allProperties'];

                    updateSensorInfo(d);
                    for (var i = 0; i < $scope.fields.length; i++) {
                        if (FilterParameters.getFields().indexOf($scope.fields[i].columnName) > -1) {
                            $scope.multipleDemo.selectedFields.push($scope.fields[i]);

                        }
                    }

                }).finally(function () {
                    $scope.loading = false;
                });

            });

            $scope.submitSelected = function () {

                var selectedColumns = [];

                for (var i = 0; i < $scope.multipleDemo.selectedFields.length; i++) {
                    selectedColumns.push($scope.multipleDemo.selectedFields[i].columnName)

                }

                FilterParameters.setFields(selectedColumns);
                FilterParameters.updateURL($location);


                sharedService.prepForBroadcast();

                //$window.location.reload();
            };

            $scope.handleClick = function (msg) {
                sharedService.prepForBroadcast(msg);
            };

        }]);


//sensorData.controller('AggregationCtrl',
//    ['$scope', 'FilterParameters',
//        function ($scope, FilterParameters) {
//
//
//            $scope.aggregationFunctions = [
//                {value: "-1", name: " No Aggregation"},
//                {value: "avg", name: "AVG"},
//                {value: "max", name: "MAX"},
//                {value: "min", name: "MIN"},
//                {value: "sum", name: "SUM"}
//            ];
//
//            $scope.selectedAggFunc = {};
//
//            $scope.aggregationPeriod = 1;
//
//            $scope.aggregationUnits = [
//                {value: "3600000", name: "Hours"},
//                {value: "60000", name: "Minutes"},
//                {value: "1000", name: "Seconds"}
//            ];
//
//            $scope.selectedAggUnit = {};
//
//            $scope.updateSensor = function (item) {
//                FilterParameters.vs = item;
//
//                MetadataLoader.loadData(item, true).then(function (d) {
//                    //$scope.metadata = d;
//
//                    updateSensorInfo(d);
//
//
//                }).finally(function () {
//                    $scope.loading = false;
//                });
//            };
//
//
//            $scope.selectedSensor = FilterParameters.vs;
//
//            $scope.loading = true;
//
//            AllSensors.loadData().then(function (d) {
//
//                $scope.sensorNames = d;
//
//
//                MetadataLoader.loadData(FilterParameters.vs).then(function (d) {
//                    //$scope.metadata = d;
//
//                    //$scope.fields = $scope.metadata.features[0].properties['allProperties'];
//
//                    updateSensorInfo(d);
//                    for (var i = 0; i < $scope.fields.length; i++) {
//                        if (FilterParameters.getFields().indexOf($scope.fields[i].columnName) > -1) {
//                            $scope.multipleDemo.selectedFields.push($scope.fields[i]);
//
//                        }
//                    }
//
//                }).finally(function () {
//                    $scope.loading = false;
//                });
//
//            });
//
//            $scope.submitSelected = function () {
//
//                var selectedColumns = [];
//
//                for (var i = 0; i < $scope.multipleDemo.selectedFields.length; i++) {
//                    selectedColumns.push($scope.multipleDemo.selectedFields[i].columnName)
//
//                }
//
//                FilterParameters.setFields(selectedColumns);
//                FilterParameters.updateURL($location);
//
//
//                sharedService.prepForBroadcast();
//
//                //$window.location.reload();
//            };
//
//            $scope.handleClick = function (msg) {
//                sharedService.prepForBroadcast(msg);
//            };
//
//        }]);


sensorData.controller('DatepickerCtrl', ['$scope', 'FilterParameters', 'SensorMetadata',
    function ($scope, FilterParameters, SensorMetadata) {

        $scope.dates = FilterParameters;


        //$scope.selectedDate = new Date();
        //$scope.selectedDateAsNumber = Date.UTC(1986, 1, 22);

        $scope.getType = function (key) {
            return Object.prototype.toString.call($scope[key]);
        };

        $scope.clearDates = function () {
            $scope.selectedDate = null;
        };

        $scope.maxDateFrom = function () {
            ////var endDate = new Date('2015-01-01');
            //var endDate = new Date();
            //if (endDate > $scope.untilDate) {
            return $scope.untilDate;
            //} else {
            //    return endDate;
            //}
        }

        $scope.minDateFrom = function () {
            var startDate = new Date('1903-01-01');
            return startDate;
        }

        //$scope.maxDateTo = function () {
        //    //var endDate = new Date('2015-01-01');
        //    var endDate = new Date();
        //
        //    return endDate;
        //}

        $scope.minDateTo = function () {
            var startDate = new Date('1903-01-01');
            if (startDate < $scope.fromDate) {
                return $scope.fromDate;
            } else {
                return startDate;
            }
        }

    }]);


sensorData.factory('FilterParameters', ['$routeParams', '$filter', function ($routeParams, $filter) {

    function FilterParameters() {

        if (!jQuery.isEmptyObject($routeParams)) {

            this.vs = $routeParams['vs'];
            this.fromDate = Date.parse($routeParams['from']);
            this.untilDate = Date.parse($routeParams['to']);

            if ($routeParams['fields'])
                this.fields = $routeParams['fields'].split(',');
            else this.fields = [];
        }

    }


    FilterParameters.prototype = {


        formatDateWeb: function (date) {
            return $filter('date')(Date.parse(date), 'yyyy-MM-dd');
        },


        getFields: function () {
            return this.fields;
        },

        setFields: function (fields) {
            this.fields = fields;
        },


        getFromDate: function () {
            if (typeof(this.fromDate) == 'number') {
                return new Date(this.fromDate);
            }
            return this.fromDate;
        },

        getUntilDate: function () {
            if (typeof(this.untilDate) == 'number') {
                return new Date(this.untilDate);
            }
            return this.untilDate;
        },

        updateURL: function (location) {
            location.search('fields', this.fields.toString());
            location.search('from', this.formatDateWeb(this.getFromDate()));
            location.search('to', this.formatDateWeb(this.getUntilDate()));
            location.search('vs', this.vs);
        }
    };

    return new FilterParameters;
}]);

