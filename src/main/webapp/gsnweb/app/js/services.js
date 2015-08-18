'use strict';

var gsnDataServices = angular.module('gsnDataServices', ['ngResource']);

gsnDataServices.factory('GsnResult', function () {

    return {
        dataMap: {},
        missingData: {},
        hasValues: false,
        pointCount: 0,

        reset: function () {
            this.dataMap = {};
            this.missingData = {};
            this.hasValues = false;
            pointCount: 0;
        }
    };
});

gsnDataServices.factory('dataProcessingService', ['UrlBuilder', '$http', 'FilterParameters', 'GsnResult',
    function (UrlBuilder, $http, FilterParameters, GsnResult) {

        var promise;

        var dataProcessingService = {
            loadData: function () {
                if (!promise) {
                    //promise = $http.get('http://montblanc.slf.ch:22001/multidata?nb=ALL&from=01/01/2015%2000:00:00&to=13/04/2015%2000:00:00&vs[0]=imis_fka_2&field[0]=rh&time_format=unix&download_format=csv&download_mode=inline&vs[1]=imis_fka_2&field[1]=ta&vs[2]=imis_fka_2&field[2]=rswr').then(function (response) {
                    var url = UrlBuilder.getGsnUrl();

                    console.log(url);

                    promise = $http.post(url).then(function (response) {


                        // The return value gets picked up by the then in the controller.
                        //return processData(response.data, fields);
                        return {status:'OK', data:response.data}
                    //},
                    //    function(reason) {
                    //    console.log('Failed: ' + reason.statusText);
                    //    return {status:'FAILED', data:reason.statusText};
                    });
                }


                // Return the promise to the controller
                return promise;
            },

            resetPromise: function () {
                promise = null;
            }
        };
        return dataProcessingService;
    }]);

gsnDataServices.factory('ProcessGsnData', ['GsnResult', 'FilterParameters',
    function (GsnResult, FilterParameters) {

        var ProcessGsnData = {
            process: function (allText) {

                GsnResult.reset();

                var headers = FilterParameters.getHeaders();
                for (var j = 0; j < headers.length; j++) {
                    GsnResult.dataMap[headers[j]] = [];
                }

                //split content based on new line
                var allTextLines = allText.split(/\r\n|\n/);

                GsnResult.pointCount = allTextLines.length;

                console.log("Loaded " + GsnResult.pointCount + " time points");
                for (var i = 0; i < allTextLines.length; i++) {
                    if (allTextLines[i].indexOf("#") === 0) {
                        continue;
                    }

                    // split content based on comma
                    var data = allTextLines[i].split(',');
                    if (data.length >= headers.length + 1) {
                        for (var j = 0; j < headers.length; j++) {
                            var tarr = [];

                            var time = parseFloat(data[0]);
                            if (!isNaN(time)) {
                                tarr.push(time);

                                var value = parseFloat(data[j + 1]);
                                if (!isNaN(value)) {
                                    tarr.push(value);
                                    delete GsnResult.missingData[headers[j]]

                                }
                                else {
                                    tarr.push(null);
                                    GsnResult.missingData[headers[j]] = true;
                                }
                                GsnResult.dataMap[headers[j]].push(tarr);
                                GsnResult.hasValues = true;
                            }


                        }
                    }
                }
                return GsnResult;

            },

            processMultiSensors: function (allText) {

                GsnResult.reset();

                //split content based on new line
                var allTextLines = allText.split(/\r\n|\n/);

                GsnResult.pointCount = allTextLines.length;

                console.log("Loaded " + GsnResult.pointCount + " time points");
                var gsnHeaders = [];

                var sensorName ='';
                var sensorsCount = 0;
                var paramCount = 0;
                for (var i = 0; i < allTextLines.length; i++) {

                    if (allTextLines[i].indexOf("# vsname") == 0) {
                        sensorName = allTextLines[i].split(":").pop();
                        sensorsCount ++;
                        paramCount = 0;
                    } else if (allTextLines[i].indexOf("# time")== 0) {
                        var parameters = allTextLines[i].split(',');
                        for (var j = 1; j < parameters.length; j++) {
                            if (parameters[j] != 'aggregation_interval') {
                                var currentHeader = parameters[j] + '_' + sensorName;
                                gsnHeaders.push(currentHeader);
                                GsnResult.dataMap[currentHeader] = [];
                                paramCount++;
                            }
                        }

                    } else if (allTextLines[i].indexOf("#") === 0) {
                        continue;
                    }

                    // split content based on comma
                    var data = allTextLines[i].split(',');
                    //if (data.length >= headers.length + 1) {
                    for (var j = 0; j < paramCount; j++) {
                        var tarr = [];

                        var index = gsnHeaders.length - paramCount + j;
                        var time = parseFloat(data[0]);
                        if (!isNaN(time)) {
                            tarr.push(time);

                            var value = parseFloat(data[j + 1]);
                            if (!isNaN(value)) {
                                tarr.push(value);
                                delete GsnResult.missingData[gsnHeaders[index]]

                            }
                            else {
                                tarr.push(null);
                                GsnResult.missingData[gsnHeaders[index]] = true;
                            }
                            GsnResult.dataMap[gsnHeaders[index]].push(tarr);
                            GsnResult.hasValues = true;
                        }


                        //}
                    }
                }
                return GsnResult;

            }
        }

        return ProcessGsnData;

    }]);

gsnDataServices.factory('UrlBuilder', ['$routeParams', '$filter', 'FilterParameters',
    function ($routeParams, $filter, FilterParameters) {

        var self = this;
        self.metatdataUrl = 'http://montblanc.slf.ch:8090/';
        //self.metatdataUrl = 'http://eflumpc18.epfl.ch/gsn/';
        //self.metatdataUrl = 'http://localhost:8090/';

        return {

            buildGsnLink: function () {
                var url = "http://montblanc.slf.ch:22001/multidata?download_format=csv";


                var count = 0;
                for (var i = 0; i < FilterParameters.sensorModels.length; i++) {
                    var model = FilterParameters.sensorModels[i];
                    for (var j = 0; j < model.parameters.selectedFields.length; j++) {
                        url += "&vs[" + count + "]=" + model.selectedSensor
                        + "&field[" + count + "]=" + model.parameters.selectedFields[j].columnName;
                        count++;

                    }

                }
                if (FilterParameters.hasAggregation()) {
                    url += "&agg_function=" + FilterParameters.getAggFuncObj().value +
                    "&agg_unit=" + FilterParameters.getAggUnitObj().value +
                    "&agg_period=" + FilterParameters.aggPeriod;
                }

                if (FilterParameters.hasDates()) {
                    url += "&from=" + this.formatDateGSN(FilterParameters.getFromDate())
                    + "&to=" + this.formatDateGSN(FilterParameters.getUntilDate());
                }
                if (FilterParameters.limitByRows) {
                    url += "&nb_value=" + FilterParameters.rowNumber + "&nb=SPECIFIED";

                } else if (!FilterParameters.hasDates()) {
                    url += "&nb_value=" + 1000 + "&nb=SPECIFIED";
                }
                return url;
            },

            getGsnUrl: function () {
                //var url = "http://montblanc.slf.ch:22001/multidata?nb=ALL&time_format=unix&download_format=csv&download_mode=inline" +
                //var url = "http://montblanc.slf.ch:22001/multidata?nb=ALL&time_format=unix&download_format=csv&download_mode=inline&agg_function=avg&agg_unit=3600000&agg_period=4" +
                var url = this.buildGsnLink();
                //url = 'http://localhost:8000/app/sensors/imis_fka_2_30min_test.txt';

                return url + '&download_mode=inline' + '&time_format=unix';
                //return 'http://localhost:8000/app/sensors/multiple_sensors_2015-06-16_22-29-23.csv';
            },

            getDwonloadUrl: function () {
                return this.buildGsnLink() + '&time_format=iso';
            },

            formatDateGSN: function (date) {
                return $filter('date')(Date.parse(date), 'dd/MM/yyyy HH:mm:ss');
            },



            sensorListUrl: function () {
                return self.metatdataUrl + 'web/virtualSensorNames';
            }

        };

    }]);


gsnDataServices.factory('ChartConfigService', ['$timeout', 'GsnResult',
    function ($timeout, GsnResult) {
        function ChartConfigService() {

            this.chartConfig;
            this.dataMap;
            this.axisInfo;
        };

        ChartConfigService.prototype = {

            setData: function (dataMap, axisInfo) {
                this.dataMap = dataMap;
                this.axisInfo = axisInfo;
            },

            buildChartConfig: function (GsnResult, axisInfo, vs_name) {
                var chartConfig = {
                    options: {
                        chart: {
                            zoomType: 'x'
                        },
                        rangeSelector: {
                            enabled: true
                        },
                        navigator: {
                            enabled: true
                        }
                    },
                    title: {
                        text: vs_name
                    },
                    useHighStocks: true,
                    yAxis: this.buildAxis(GsnResult, axisInfo),
                    series: this.buildSeries(GsnResult, axisInfo),
                    tooltip: {
                        shared: true
                    },
                    legend: {
                        layout: 'vertical',
                        align: 'left',
                        x: 80,
                        verticalAlign: 'top',
                        y: 55,
                        floating: true,
                        backgroundColor: (Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'
                    }
                    ,
                    func: function (chart) {
                        $timeout(function () {
                            chart.reflow();
                            //The below is an event that will trigger all instances of charts to reflow
                            //$scope.$broadcast('highchartsng.reflow');
                        }, 0);
                    }
                }
                this.chartConfig = chartConfig;

                return chartConfig;
            },

            buildSeries: function (GsnResult, axisInfo) {
                var series = [];
                var i = 0;
                for (var key in GsnResult.dataMap) {
                    if (GsnResult.dataMap.hasOwnProperty(key) && !GsnResult.missingData[key]) {
                        var single = {};
                        single['name'] = axisInfo[key]['name'];
                        single['type'] = 'spline';
                        single['yAxis'] = i++;
                        single['data'] = GsnResult.dataMap[key];
                        single['tooltip'] = {'valueSuffix': axisInfo[key]['unit'], 'valueDecimals': 2};
                        series.push(single);
                    }
                }
                return series;
            },

            buildAxis: function (GsnResult, axisInfo) {
                var axis = [];
                var i = 0;
                for (var key in GsnResult.dataMap) {
                    if (GsnResult.dataMap.hasOwnProperty(key) && !GsnResult.missingData[key]) {
                        var color = Highcharts.getOptions().colors[i++];
                        var style = {'color': color};

                        var title = {'text': axisInfo[key]["name"], 'style': style};
                        var label = {'format': '{value}' + axisInfo[key]["unit"], 'style': style};

                        var single = {'title': title, 'labels': label};

                        axis.push(single);
                    }
                }
                return axis;
            }

        };
        return new ChartConfigService;
    }])




