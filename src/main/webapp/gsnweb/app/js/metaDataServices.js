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
        var SensorMetadata = {
            metadata: null,

            init: function(data) {
                this.metadata = data;
            },

            getProperties: function() {
                return this.metadata.features[0].properties['allProperties'];
            },

            getSensorName: function() {
                $scope.metadata.features[0].properties.sensorName;
            },

            getFormDate: function() {

            },

            getToDate: function() {

            }


        }

        return SensorMetadata;
    })