'use strict';

var metaDataServices = angular.module('metaDataServices', [])

    .factory('MetadataLoader', ['UrlBuilder', '$http', function (UrlBuilder, $http) {

        var promise;

        var MetadataLoader = {
            loadData: function (sensorName, reset) {
                if (!promise || reset) {
                    var url = UrlBuilder.getMetaDataUrl(sensorName);
                    console.log(url);

                    promise = $http.get(url).then(function (response) {

                        return response.data;
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