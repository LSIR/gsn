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
    ['$scope', '$window', 'MetadataLoader', '$location', 'sharedService', 'UrlBuilder', 'FilterParameters', 'AllSensors', 'SensorMetadata', 'Aggregation',
        function ($scope, $window, MetadataLoader, $location, sharedService, UrlBuilder, FilterParameters, AllSensors, SensorMetadata, Aggregation) {


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


            $scope.aggregationFunctions = Aggregation.aggFunctions();


            $scope.aggregationUnits = Aggregation.aggUnits();

            $scope.selectedSensor = FilterParameters.vs;

            $scope.aggFunc = {};
            $scope.aggFunc.selected = FilterParameters.getAggFuncObj();
            $scope.aggregationPeriod = FilterParameters.aggPeriod;
            $scope.aggUnit = {};
            $scope.aggUnit.selected = FilterParameters.getAggUnitObj();


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

            function updateFilterParameters() {
                var selectedColumns = [];

                for (var i = 0; i < $scope.multipleDemo.selectedFields.length; i++) {
                    selectedColumns.push($scope.multipleDemo.selectedFields[i].columnName)

                }

                FilterParameters.aggFunc = $scope.aggFunc.selected.value;

                if ($scope.aggFunc.selected.value != -1) {
                    FilterParameters.aggPeriod = $scope.aggregationPeriod;
                    FilterParameters.aggUnit = $scope.aggUnit.selected.name;
                } else {
                    FilterParameters.aggPeriod = 1;
                    FilterParameters.aggUnit = Aggregation.aggUnits()[0].name;
                }

                FilterParameters.setFields(selectedColumns);
                FilterParameters.updateURL($location);
            }

            $scope.submitSelected = function () {

                updateFilterParameters();
                sharedService.prepForBroadcast();

                //$window.location.reload();
            };

            $scope.handleClick = function (msg) {
                sharedService.prepForBroadcast(msg);
            };

            $scope.download = function () {
                updateFilterParameters();
                $window.location.href = UrlBuilder.getDwonloadUrl();
            };


        }]);

sensorData.factory('Aggregation', function () {
    var self = this;
    self.aggFunc = [
        {value: '-1', name: ' No Aggregation'},
        {value: 'avg', name: 'AVG'},
        {value: 'max', name: 'MAX'},
        {value: 'min', name: 'MIN'},
        {value: 'sum', name: 'SUM'}
    ];

    self.aggUnits = [
        {value: '3600000', name: 'hours'},
        {value: '60000', name: 'minutes'},
        {value: '1000', name: 'seconds'}
    ];

    var Aggregation = {
        aggFunctions: function () {
            return self.aggFunc;
        },

        aggUnits: function () {
            return self.aggUnits;
        },

        getFuncByValue: function (value) {
            if (!value) {
                return self.aggFunc[0];
            }
            for (var i = 0; i < self.aggFunc.length; i++) {
                if (self.aggFunc[i].value == value) {
                    return self.aggFunc[i];
                }
            }
            return self.aggFunc[0];

        },

        getUnitByName: function (value) {
            if (!value) {
                return self.aggUnits[0];
            }
            for (var i = 0; i < self.aggUnits.length; i++) {
                if (self.aggUnits[i].name == value) {
                    return self.aggUnits[i];
                }
            }
            return self.aggUnits[0];

        }
    }

    return Aggregation;
});

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


sensorData.factory('FilterParameters', ['$routeParams', '$filter', 'Aggregation',
    function ($routeParams, $filter, Aggregation) {

        function FilterParameters() {

            if (!jQuery.isEmptyObject($routeParams)) {

                this.vs = $routeParams['sensors'];
                this.fromDate = Date.parse($routeParams['from']);
                this.untilDate = Date.parse($routeParams['to']);

                if ($routeParams['parameters'])
                    this.fields = $routeParams['parameters'].split(',');
                else this.fields = [];


                this.aggFunc = $routeParams['aggFunc'];
                this.aggUnit = $routeParams['aggUnit'];
                this.aggPeriod = $routeParams['aggPeriod'];

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

            hasAggregation: function () {
                return this.aggFunc != -1;
            },

            getAggFuncObj: function () {
                return Aggregation.getFuncByValue(this.aggFunc);

            },

            getAggUnitObj: function () {
                return Aggregation.getUnitByName(this.aggUnit);

            },

            updateURL: function (location) {
                location.search('parameters', this.fields.toString());
                location.search('from', this.formatDateWeb(this.getFromDate()));
                location.search('to', this.formatDateWeb(this.getUntilDate()));
                location.search('sensors', this.vs);
                if (this.hasAggregation()) {
                    location.search('aggFunc', this.aggFunc);
                    location.search('aggUnit', this.aggUnit);
                    location.search('aggPeriod', this.aggPeriod);
                } else {
                    location.search('aggFunc', null);
                    location.search('aggUnit', null);
                    location.search('aggPeriod', null);
                }
            }
        };

        return new FilterParameters;
    }]);

