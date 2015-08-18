'use strict';

var allSensors = angular.module('allSensors', []);

allSensors.factory('AllSensors', ['UrlBuilder', '$http',
    function (UrlBuilder, $http) {
        var promiseNames;
        var promiseWithPrivacy;

        var AllSensors = {
            loadNames: function () {
                if (!promiseNames) {

                    var url = UrlBuilder.sensorListUrl();

                    console.log(url);

                    promiseNames = $http.get(url).then(function (response) {

                        // $http returns a promise, which has a then function, which also returns a promise
                        // The then function here is an opportunity to modify the response
                        return response.data;

                    });
                }
                // Return the promise to the controller
                return promiseNames;
            },

            loadSensorsWithPrivacy: function() {
                if (!promiseWithPrivacy) {

                    //var url = 'http://localhost:8090/web/virtualSensorNamesWithPrivacy';
                    //var url = 'http://eflumpc18.epfl.ch/gsn/web/virtualSensorNamesWithPrivacy';
                    var url = 'http://montblanc.slf.ch:8090/web/virtualSensorNamesWithPrivacy';

                    console.log(url);

                    promiseWithPrivacy = $http.get(url).then(function (response) {
                        return response.data;
                    });
                }
                return promiseWithPrivacy;
            }


        };

        return AllSensors;
    }]);

