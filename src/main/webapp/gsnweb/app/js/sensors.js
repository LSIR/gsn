'use strict';

var allSensors = angular.module('allSensors', []);

allSensors.factory('AllSensors', ['UrlBuilder', '$http',
    function (UrlBuilder, $http) {
        var promise;

        var AllSensors = {
            loadData: function () {
                if (!promise) {

                    var url = UrlBuilder.sensorListUrl();

                    console.log(url);

                    promise = $http.get(url).then(function (response) {

                        // $http returns a promise, which has a then function, which also returns a promise
                        // The then function here is an opportunity to modify the response
                        return response.data;

                    });
                }
                // Return the promise to the controller
                return promise;
            },

            resetPromise: function() {
                promise = null;
            }

        };

        return AllSensors;
    }]);

