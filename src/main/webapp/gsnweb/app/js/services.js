'use strict';

var gsnDataServices = angular.module('gsnDataServices', ['ngResource']);

gsnDataServices.factory('GsnResult', function () {

    return {
        dataMap: {},
        missingData: {},
        hasValues: false,

        reset: function () {
            this.dataMap = {};
            this.missingData = {}
            this.hasValues = false;
        }
    };
});

gsnDataServices.factory('dataProcessingService', ['UrlBuilder', '$http', 'FilterParameters', 'GsnResult',
    function (UrlBuilder, $http, FilterParameters, GsnResult) {

        var promise;

        var processData = function (allText, headers) {
            GsnResult.reset();

            for (var j = 0; j < headers.length; j++) {
                GsnResult.dataMap[headers[j]] = [];
            }

            //split content based on new line
            var allTextLines = allText.split(/\r\n|\n/);

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
        };

        var dataProcessingService = {
            async: function () {
                if (!promise) {
                    //promise = $http.get('http://montblanc.slf.ch:22001/multidata?nb=ALL&from=01/01/2015%2000:00:00&to=13/04/2015%2000:00:00&vs[0]=imis_fka_2&field[0]=rh&time_format=unix&download_format=csv&download_mode=inline&vs[1]=imis_fka_2&field[1]=ta&vs[2]=imis_fka_2&field[2]=rswr').then(function (response) {

                    var url = UrlBuilder.getGsnUrl();

                    console.log(url);

                    var fields = FilterParameters.getFields();

                    promise = $http.get(url).then(function (response) {


                        // The return value gets picked up by the then in the controller.
                        return processData(response.data, fields);
                    });
                }


                // Return the promise to the controller
                return promise;
            }
            ,

            resetPromise: function () {
                promise = null;
            }
        };
        return dataProcessingService;
    }]);


gsnDataServices.factory('AxisInfo', ['UrlBuilder', '$http',
    function (UrlBuilder, $http) {
        var promise;

        var AxisInfo = {
            getAxesInfo: function () {
                if (!promise) {

                    var url = UrlBuilder.getTaxonomyUrl();

                    console.log(url);

                    promise = $http.get(url).then(function (response) {

                        return response.data;

                    });
                }
                // Return the promise to the controller
                return promise;
            },

            resetPromise: function () {
                promise = null;
            }

        };

        return AxisInfo;
    }]);


gsnDataServices.factory('UrlBuilder', ['$routeParams', '$filter', 'FilterParameters',
    function ($routeParams, $filter, FilterParameters) {

        var self = this;
        //self.metatdataUrl = 'http://eflumpc18.epfl.ch/gsn/';
        self.metatdataUrl = 'http://localhost:8090/';

        return {

            getGsnUrl: function () {
                //var url = "http://montblanc.slf.ch:22001/multidata?nb=ALL&time_format=unix&download_format=csv&download_mode=inline" +
                var url = "http://montblanc.slf.ch:22001/multidata?nb=ALL&time_format=unix&download_format=csv&download_mode=inline&agg_function=avg&agg_unit=3600000&agg_period=4" +
                    "&from=" + this.formatDateGSN(FilterParameters.getFromDate())
                    + "&to=" + this.formatDateGSN(FilterParameters.getUntilDate());
                for (var i = 0; i < FilterParameters.getFields().length; i++) {
                    url += "&vs[" + i + "]=" + FilterParameters.vs
                    + "&field[" + i + "]=" + FilterParameters.getFields()[i];

                }

                //url = 'http://localhost:8000/app/sensors/imis_fka_2_30min_test.txt';
                return url;
            },

            formatDateGSN: function (date) {
                return $filter('date')(Date.parse(date), 'dd/MM/yyyy HH:mm:ss');
            },


            getMetaDataUrl: function (sensorName) {
                //return "http://eflumpc18.epfl.ch/gsn/web/virtualSensors/" + this.vs;
                return  self.metatdataUrl + "web/virtualSensors/" + sensorName;
            },

            sensorListUrl: function() {
                return self.metatdataUrl + 'web/virtualSensorNames';
            },

            getTaxonomyUrl: function () {
                var url = self.metatdataUrl + "taxonomy/columnData?sensorName=" + FilterParameters.vs +
                    "&columnNames=";
                for (var i = 0; i < FilterParameters.getFields().length; i++) {
                    url += "&columnNames=" + FilterParameters.getFields()[i];
                }
                return url;
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




