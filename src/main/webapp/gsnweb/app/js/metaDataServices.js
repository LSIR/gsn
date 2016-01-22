'use strict';

var metaDataServices = angular.module('metaDataServices', [])

    .factory('MetadataLoader', [ '$http', '$q',  function ( $http, $q) {

        var promise;

        var self = this;
        self.metatdataUrl = 'http://montblanc.slf.ch:8090/';
        //self.metatdataUrl = 'http://eflumpc18.epfl.ch/gsn/';
        //self.metatdataUrl = 'http://localhost:8090/';

        var MetadataLoader = {
            loadData: function (sensorName, reset) {
                if (!promise || reset) {
                    //var url = UrlBuilder.getMetaDataUrl(sensorName);
                    var url = self.metatdataUrl + "web/virtualSensors/" + sensorName;
                    console.log(url);

                    promise = $http.get(url).then(function (response) {

                        return response.data;
                    }, function(reason) {
                        console.log('ERROR : ' + reason.data);
                    });

                }
                return promise;
            }


        };
        return MetadataLoader;
    }])

    .factory('SensorMetadata', function(){
        function SensorMetadata(metadata) {
            this.metadata = metadata;
        }

        SensorMetadata.prototype = {

            init: function(data) {
                this.metadata = data;
            },

            getProperties: function() {
                return this.metadata.features[0].properties['allProperties'];
            },

            getSensorName: function() {
                return this.metadata.features[0].properties.sensorName;
            },

            getFromDate: function() {
                //return new Date('2001-01-01');
                return this.metadata.features[0].properties['fromDate'];
            },

            getToDate: function() {
                //return new Date('2016-01-01');
                return this.metadata.features[0].properties['untilDate'];
            }


        }

        return SensorMetadata;
    })