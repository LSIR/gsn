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
    ['$scope', '$window', 'MetadataLoader', '$location', 'UrlBuilder', 'FilterParameters', 'AllSensors', 'SensorMetadata', 'Aggregation', 'SensorModel', '$q',
        function ($scope, $window, MetadataLoader, $location,  UrlBuilder, FilterParameters, AllSensors, SensorMetadata, Aggregation, SensorModel, $q) {

            $scope.rowNumber = FilterParameters.rowNumber;
            $scope.limitByRows = FilterParameters.limitByRows;

            $scope.limitRowNumber = !FilterParameters.hasDates();

            $scope.aggregationFunctions = Aggregation.aggFunctions();
            $scope.aggregationUnits = Aggregation.aggUnits();
            $scope.aggFunc = {};
            $scope.aggFunc.selected = FilterParameters.getAggFuncObj();
            $scope.aggregationPeriod = FilterParameters.aggPeriod;
            $scope.aggUnit = {};
            $scope.aggUnit.selected = FilterParameters.getAggUnitObj();


            $scope.sensorsWithParameters = [];

            $scope.loading = true;


            AllSensors.loadSensorsWithPrivacy().then(function (d) {
                $scope.sensorNames = d;


                if (FilterParameters.sensors.length < 1) {
                    $scope.plotAvailable = false;
                    $scope.sensorsWithParameters.push(new SensorModel());
                    return;
                }

                FilterParameters.getSensorModels().then(function (sensorModels) {
                    $scope.sensorsWithParameters = FilterParameters.sensorModels;
                    $scope.plotAvailable = true

                }).finally(function () {
                    $scope.loading = false;
                });

            });

            $scope.updateParameter = function (item) {
                $scope.plotAvailable = true;
            };


            $scope.nameGroupFn = function (item) {
                return item.name;
            };

            $scope.propWithUnit = function (item) {
                return item.columnName + "(" + item.unit + ")";
            };

            $scope.getSensorIcon = function(sensor) {
                if(sensor.property) {
                    return 'img/green_.png';
                } else {
                    return 'img/red_.png';
                }
            }

            function updateSensorInfo(sensor, index) {
                var metadata = new SensorMetadata(sensor);

                $scope.sensorsWithParameters[index].update(metadata);


            }

            $scope.updateSensor = function (item, index) {
                FilterParameters.vs = item.name;
                $scope.sensorsWithParameters[index].selectedSensor = item.name;


                MetadataLoader.loadData(item.name, true).then(function (d) {
                    //$scope.metadata = d;

                    updateSensorInfo(d, index);

                    $scope.plotAvailable = false;


                }).finally(function () {
                    $scope.loading = false;
                });
            };

            $scope.addSensor = function () {
                $scope.sensorsWithParameters.push(new SensorModel());
            };

            $scope.removeSensor = function (index) {
                $scope.sensorsWithParameters.splice(index, 1);
            };

            function updateFilterParameters() {

                FilterParameters.sensorModels = $scope.sensorsWithParameters;

                var selectedColumns = [];

                for (var i = 0; i < $scope.sensorsWithParameters[0].parameters.selectedFields.length; i++) {
                    selectedColumns.push($scope.sensorsWithParameters[0].parameters.selectedFields[i].columnName)

                }
                FilterParameters.setFields(selectedColumns);


                FilterParameters.aggFunc = $scope.aggFunc.selected.value;

                if ($scope.aggFunc.selected.value != -1) {
                    FilterParameters.aggPeriod = $scope.aggregationPeriod;
                    FilterParameters.aggUnit = $scope.aggUnit.selected.name;
                } else {
                    FilterParameters.aggPeriod = 1;
                    FilterParameters.aggUnit = Aggregation.aggUnits()[0].name;
                }

                FilterParameters.limitByRows = $scope.limitByRows;

                if ($scope.limitByRows) {
                    FilterParameters.rowNumber = $scope.rowNumber;
                }

                FilterParameters.updateURL($location);
                //FilterParameters.resetPromise();
            }

            $scope.submitSelected = function () {

                updateFilterParameters();

            };


            $scope.download = function () {
                updateFilterParameters();
                $window.location.href = UrlBuilder.getDwonloadUrl();
            };


        }]);

sensorData.factory('SensorModel', function () {
    function SensorModel() {
        this.selectedSensor;
        this.parameters = {};
        this.parameters.selectedFields = [];
        this.from = '';
        this.until = '';
        this.fields = [];


    }

    SensorModel.prototype = {

        update: function (metadata) {
            this.fields = metadata.getProperties();

            this.parameters = {};
            this.parameters.selectedFields = [];

            this.from = metadata.getFromDate();
            this.until = metadata.getToDate();
        },

        isSensorSelected: function () {
            return this.selectedSensor;
        }
    };

    return SensorModel;
});

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
    };

    return Aggregation;
});

sensorData.controller('DatepickerCtrl', ['$scope', 'FilterParameters', 'SensorMetadata',
    function ($scope, FilterParameters, SensorMetadata) {

        $scope.dates = FilterParameters;


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


sensorData.factory('FilterParameters', ['$routeParams', '$filter', 'Aggregation', 'SensorModel', 'SensorMetadata', 'MetadataLoader', '$q',
    function ($routeParams, $filter, Aggregation, SensorModel, SensorMetadata, MetadataLoader, $q) {

        function init() {
            if ($routeParams['sensors'])
                this.sensors = $routeParams['sensors'].split(',');
            else this.sensors = [];

            this.vs = $routeParams['sensors'];
            this.fromDate = Date.parse($routeParams['from']);
            this.untilDate = Date.parse($routeParams['to']);

            if ($routeParams['parameters'])
                this.fields = $routeParams['parameters'].split(',');
            else this.fields = [];


            this.aggFunc = $routeParams['aggFunc'];
            this.aggUnit = $routeParams['aggUnit'];
            this.aggPeriod = $routeParams['aggPeriod'];

            if ($routeParams['rowNumber']) {
                this.rowNumber = $routeParams['rowNumber'];
                this.limitByRows = true;
            } else {
                this.rowNumber = 100;
                this.limitByRows = !this.hasDates();
            }
            ;

            this.sensorModels = [];
        }

        function FilterParameters() {

            //if (!jQuery.isEmptyObject($routeParams)) {
            init.call(this);
            //this.resetPromise();
        //}

        }


        FilterParameters.prototype = {

            reset: function() {
                init.call(this);
            },

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

            hasDates: function () {
                return this.untilDate && this.fromDate;
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

            hasRequiredParameters: function () {
                return this.sensors.length > 0 && this.fields.length > 0 &&
                    (this.hasDates() || this.limitByRows);
            },

            getSensorModels: function () {


                if (!this.promise) {

                    var promises = [];

                    for (var s = 0; s < this.sensors.length; s++) {

                        var index = s;
                        var sensor = this.sensors[index];

                        if (sensor && sensor.length > 0) {

                            var sensorModel = new SensorModel();
                            sensorModel.selectedSensor = sensor;
                            this.sensorModels.push(sensorModel);

                            promises.push(MetadataLoader.loadData(sensor, true));
                        }

                    }

                    var self = this;

                    this.promise = $q.all(promises).then(function (data) {

                        for (var index = 0; index < data.length; index++) {

                            if (data[index]) {
                                var metadata = new SensorMetadata(data[index]);
                                self.sensorModels[index].update(metadata);

                                for (var i = 0; i < self.sensorModels[index].fields.length; i++) {
                                    if (self.getFields().indexOf(self.sensorModels[index].fields[i].columnName) > -1) {
                                        self.sensorModels[index].parameters.selectedFields.push(self.sensorModels[index].fields[i]);

                                    }
                                }
                            }

                        }

                        return self.sensorModels;

                    });
                }

                return this.promise;
            },

            resetPromise: function () {
                this.promise = null;
            },

            getAllSelectedParameters: function () {
                var parameters = {};
                this.sensorModels.forEach(function (sensor) {
                    sensor.parameters.selectedFields.forEach(function (selectedParameter) {
                        var parameter = {
                            name: selectedParameter.name + '(' + sensor.selectedSensor + ')',
                            unit: selectedParameter.unit
                        }

                        parameters[selectedParameter.columnName + '_' + sensor.selectedSensor] = parameter;
                    })

                });
                return parameters;
            },

            getHeaders: function () {
                var headers = [];
                this.sensorModels.forEach(function (sensor) {
                    sensor.parameters.selectedFields.forEach(function (selectedParameter) {
                        headers.push(selectedParameter.columnName + '_' + sensor.selectedSensor);
                    })

                });

                return headers;
            },

            updateURL: function (location) {
                this.fields = [];
                this.sensors = [];
                for (var i = 0; i < this.sensorModels.length; i++) {
                    var selectedSensor = this.sensorModels[i].selectedSensor;
                    if (selectedSensor && selectedSensor.length > 0) {
                        for (var j = 0; j < this.sensorModels[i].parameters.selectedFields.length; j++) {
                            this.fields.push(this.sensorModels[i].parameters.selectedFields[j].columnName);
                        }
                        this.sensors.push(selectedSensor);
                    } else {
                        this.sensorModels.splice(i, 1);
                    }
                }

                location.search('parameters', this.fields.toString());
                if (this.hasDates()) {
                    location.search('from', this.formatDateWeb(this.getFromDate()));
                    location.search('to', this.formatDateWeb(this.getUntilDate()));
                }
                location.search('sensors', this.sensors.toString());
                if (this.hasAggregation()) {
                    location.search('aggFunc', this.aggFunc);
                    location.search('aggUnit', this.aggUnit);
                    location.search('aggPeriod', this.aggPeriod);
                } else {
                    location.search('aggFunc', null);
                    location.search('aggUnit', null);
                    location.search('aggPeriod', null);
                }

                if (this.limitByRows) {
                    location.search('rowNumber', this.rowNumber);
                } else {
                    location.search('rowNumber', null);
                }
            },

            updateURLFromMap: function (location) {

                location.search('parameters', this.fields.toString());

                location.search('sensors', this.sensors.toString());

            },

            setSensorFields: function (sensorFields) {
                this.sensorFields = sensorFields;
            },

            getSensorFields: function () {
                return this.sensorFields;
            }
        };

        return new FilterParameters;
    }]);

