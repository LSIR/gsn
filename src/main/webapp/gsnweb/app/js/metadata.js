var metadata = angular.module("metadata", [])

.controller("MetadataController", ["$scope", '$location', 'difMetadata',
    function ($scope, $location, difMetadata) {

        $scope.hasDif = function() {
            return $scope.hasMetadata() && difMetadata.data.dif != "";
        };

        $scope.hasMetadata = function() {
            return typeof difMetadata.data != 'undefined'
        };

        $scope.hasGSNMetadata = function() {
            return $scope.hasMetadata() && difMetadata.data.gsn != "";        };

        $scope.hasWikiLink= function() {
            return  $scope.gsnMetadata.features[0].properties.wikiLink  != undefined;        };

        if ($scope.hasDif()) {
            $scope.dif = JSON.parse(difMetadata.data.dif);
        }

        if ($scope.hasGSNMetadata() ) {
            $scope.gsnMetadata = JSON.parse(difMetadata.data.gsn);
        }


    }])

    .factory('DifMetadataLoad', ['$http', '$q', '$route',
    function ($http, $q, $route) {

        this.promise;

        var self = this;
        var sdo = {
            getData: function () {

                //if (!self.promise) {
                    var sensorName = $route.current.params.sensor;

                if (sensorName == null) {
                    return "No metadata available";
                }
                    //var url = 'http://localhost:8090/web/metadatadif/' + sensorName;
                    var url = 'http://montblanc.slf.ch:8090/web/metadatadif/' + sensorName;

                    self.promise = $http({
                        method: 'GET',
                        url: url
                    });
                    self.promise.then(function (data) {
                        return data.response;
                    }, function(data) {
                        console.log('ERROR ' + data.statusText );
                        return 'No metadata available';
                    });
                //}
                return self.promise;
            }
        };
        return sdo;
    }])

.factory('DifMetadata', function(){
            function DifMetadata(metadata) {
                this.metadata = metadata;
            }

            DifMetadata.prototype = {


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

            return DifMetadata;
        })
;
