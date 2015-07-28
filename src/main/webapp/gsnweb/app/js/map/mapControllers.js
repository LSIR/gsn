var gsnMap = angular.module("gsnMap", ["leaflet-directive"]);

gsnMap.controller("GoogleMapsController", ["$scope", 'leafletData', '$compile', '$filter', 'sensors', 'FilterParameters', 'sharedService', '$location', '_', 'MapFilterParameters',
    function ($scope, leafletData, $compile, $filter, sensors, FilterParameters, sharedService, $location, _, MapFilterParameters) {


        $scope.geojson = {};
        $scope.data = sensors.data;
        $scope.geojson.data = sensors.data;
        $scope.features = sensors.data.features;


        $scope.center = {
            lat: 46.4,
            lng: 8,
            zoom: 8
        };

        var namesOfGroup = {};
        var parametersOfGroup = {};

        var namesOfGroupPublic = {};
        var parametersOfGroupPublic = {};

        for (var i = 0; i < $scope.features.length; i++) {
            var properties = $scope.features[i].properties;
            if (!(namesOfGroup[properties.group])) {
                namesOfGroup[properties.group] = [];
                parametersOfGroup[properties.group] = [];

            }
            namesOfGroup[properties.group].push(properties.sensorName);
            parametersOfGroup[properties.group] = _.union(parametersOfGroup[properties.group], properties.observed_properties);

            if (properties.isPublic) {
                if (!(namesOfGroupPublic[properties.group])) {
                    namesOfGroupPublic[properties.group] = [];
                    parametersOfGroupPublic[properties.group] = [];
                }
                namesOfGroupPublic[properties.group].push(properties.sensorName);
                parametersOfGroupPublic[properties.group] = _.union(parametersOfGroup[properties.group], properties.observed_properties);
            }
        }

        function updateFilterParameters(namesOfGroup, parametersOfGroup) {
            $scope.groups = ['All'].concat(_.keys(namesOfGroup).sort());
            $scope.sensorNames = [].concat.apply([], _.values(namesOfGroup).sort());
            $scope.parameters = _.uniq([].concat.apply([], _.values(parametersOfGroup)).sort(), true);
        }


        $scope.updateGroup = function (item) {
            if (item === 'All') {
                $scope.sensorNames = [].concat.apply([], _.values(namesOfGroup).sort());
                $scope.parameters = _.uniq([].concat.apply([], _.values(parametersOfGroup)).sort(), true);
            } else {
                $scope.sensorNames = [].concat(namesOfGroup[item].sort());
                $scope.parameters = _.uniq([].concat(parametersOfGroup[item].sort()), true);

            }
            $scope.filter.sensors = [];
            updateMarkers();
        };

        $scope.changePrivacy = function () {
            if ($scope.filter.onlyPublic) {
                updateFilterParameters(namesOfGroupPublic, parametersOfGroupPublic);
            } else {
                updateFilterParameters(namesOfGroup, parametersOfGroup);
            }

            updateMarkers();

        };

        angular.extend($scope, {

            layers: {
                baselayers: {
                    googleTerrain: {
                        name: 'Google Terrain',
                        layerType: 'TERRAIN',
                        type: 'google'
                    },
                    googleHybrid: {
                        name: 'Google Hybrid',
                        layerType: 'HYBRID',
                        type: 'google'
                    },
                    googleRoadmap: {
                        name: 'Google Streets',
                        layerType: 'ROADMAP',
                        type: 'google'
                    }
                }
            }

        });

        $scope.filter = MapFilterParameters;

        $scope.submit = function () {
            updateMarkers();
        };

        $scope.currentMarkers = L.markerClusterGroup();


        function updateMarkers() {
            //var markers = L.markerClusterGroup();

            var iconOpen = {
                iconUrl: 'img/green_.png',
                iconSize: [28, 28],
                iconAnchor: [12, 0]
            };
            var iconProtected = {
                iconUrl: 'img/red_.png',
                iconSize: [28, 28],
                iconAnchor: [12, 0]
            };


            var geoJsonLayer = L.geoJson(sensors.data, {
                onEachFeature: onEachFeature,
                filter: function (feature, layer) {
                    return filterSensor(feature);
                },
                pointToLayer: function (feature, latlng) {
                    if (feature.properties.isPublic) {
                        return new L.marker(latlng, {icon: L.icon(iconOpen)});
                    } else {
                        return new L.marker(latlng, {icon: L.icon(iconProtected)});
                    }
                }
            });


            $scope.currentMarkers.clearLayers();
            $scope.currentMarkers.addLayer(geoJsonLayer);
            leafletData.getMap().then(function (map) {
                //map.removeLayer($scope.currentMarkers);
                map.addLayer($scope.currentMarkers);
                map.fitBounds($scope.currentMarkers.getBounds());
            });

        }

        $scope.changePrivacy();


        function filterSensor(feature) {
            var result = true;

            if ($scope.filter.sensors.length > 0) {
                var anySensor = false;
                for (var i = 0; i < $scope.filter.sensors.length; i++) {
                    anySensor = anySensor || (feature.properties.sensorName === $scope.filter.sensors[i]);
                }
                result = result && anySensor;
            }
            if ($scope.filter.group.selected && $scope.filter.group.selected != 'All') {
                result = result && (feature.properties.group === $scope.filter.group.selected);
            }

            if ($scope.filter.parameters.length > 0) {
                //OR logic
                //var anyParam= false;
                //for (var j = 0; j < $scope.filter.parameters.length; j++) {
                //    anyParam = anyParam || (feature.properties.observed_properties.indexOf($scope.filter.parameters[j]) > -1);
                //}
                //result = result && anyParam;

                //AND logic
                for (var j = 0; j < $scope.filter.parameters.length; j++) {
                    result = result && (feature.properties.observed_properties.indexOf($scope.filter.parameters[j]) > -1);
                }
            }
            if ($scope.filter.onlyPublic) {
                result = result && feature.properties.isPublic;
            }

            return result;
        }

        //
        //function onEachFeature(feature, layer) {
        //    layer.on('click', function (e) {
        //        var sensorName = feature.properties.sensorName;
        //
        //        //var html = '<div><b>{{sensorName}}</b><br><a href="#/plot?sensors={{sensorName}}&parameters={{parameters}}" my-refresh>Plot</a></div>';
        //        //var html = '<div><b>{{sensorName}}</b><p>Parameters</p><ul><li ng-repeat="param in parameters">{{param}}</li></ul><br><md-button class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';
        //
        //        var html = '<div><b>{{extra}}</b><p>Parameters</p><table><tr ng-repeat="param in parameters"><td>{{param.name}}</td></tr></table><ul><li ng-repeat="param in parameters">{{param}}</li></ul><br><md-button class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';
        //
        //        LatestData.resetPromise();
        //        LatestData.getData(sensorName).then(function (data) {
        //            console.log(data);
        //            var newScope = $scope.$new();
        //            newScope.sensorName = sensorName;
        //            newScope.feature = feature;
        //            newScope.parameters = data.properties.fields;
        //            newScope.values = data.properties.values;
        //
        //            //newScope.parameters = feature.properties.observed_properties;
        //
        //            var linkFunction = $compile(html)(newScope);
        //
        //            layer.bindPopup(linkFunction[0]);
        //        });
        //    });
        //}

        function onEachFeature(feature, layer) {
            var sensorName = feature.properties.sensorName;

            //var html = '<div><b>{{sensorName}}</b><br><a href="#/plot?sensors={{sensorName}}&parameters={{parameters}}" my-refresh>Plot</a></div>';
            var html = '<div><b>{{sensorName}}</b></br><i>has data from {{fromDate}} to {{toDate}}</i><p>Parameters</p><ul><li ng-repeat="param in parameters">{{param}}</li></ul><br><md-button ng-disabled="protected" class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';

            //var html = '<div><b>{{extra}}</b><p>Parameters</p><table><tr ng-repeat="param in parameters"><td>{{param.name}}</td></tr></table><ul><li ng-repeat="param in parameters">{{param}}</li></ul><br><md-button class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';

            //LatestData.resetPromise();
            //LatestData.getData(sensorName).then(function (data) {
            var newScope = $scope.$new();
            newScope.sensorName = sensorName;
            newScope.feature = feature;
            newScope.protected = !feature.properties.isPublic;
            newScope.fromDate = feature.properties.fromDate;
            newScope.toDate = feature.properties.untilDate;
            //newScope.parameters = data.properties.fields;
            //newScope.values = data.properties.values;

            newScope.parameters = feature.properties.observed_properties;

            var linkFunction = $compile(html)(newScope);

            layer.bindPopup(linkFunction[0]);
            //});

        }

        $scope.plot = function (feature) {
            console.log('PLOT ' + feature.properties.sensorName);
            FilterParameters.reset();
            FilterParameters.sensors = [feature.properties.sensorName];
            FilterParameters.fields = getParametersForLink(feature)
            FilterParameters.resetPromise();
            $location.path('/plot')
            FilterParameters.updateURLFromMap($location);
            sharedService.prepForBroadcast();

        };

        function getParametersForLink(feature) {
            var colNames = [];
            for (var i = 0; i < $scope.filter.parameters.length; i++) {
                colNames = _.union(colNames, feature.properties[$scope.filter.parameters[i]])
            }
            return colNames;
        }


    }

])
;


gsnMap.directive('myRefresh', function ($location, $route) {
    return function (scope, element, attrs) {
        element.bind('click', function () {
            if (element[0] && element[0].href && element[0].href === $location.absUrl()) {
                $route.reload();
            }
        });
    }
});


gsnMap.factory('Sensors', ['$http', function ($http) {
    var sdo = {
        getSensors: function () {
            var promise = $http({
                method: 'GET',
                url: 'http://eflumpc18.epfl.ch/gsn/web/virtualSensors?onlyPublic=false'
                //url: 'http://eflumpc18.epfl.ch/gsn/web/virtualSensors'
                //url: 'http://localhost:8090/web/virtualSensors?onlyPublic=false'
            });
            promise.success(function (data, status, headers, conf) {
                return data;
            });
            return promise;
        }
    };
    return sdo;
}]);

gsnMap.factory('LatestData', ['$http', function ($http) {
    var promise;

    var dataProcessingService = {
        getData: function (sensorName) {
            if (!promise) {

                var url = 'http://montblanc.slf.ch:22002/ws/api/sensors/' + sensorName + '?latestValues=true';

                promise = $http.get(url).then(function (response) {


                    // The return value gets picked up by the then in the controller.
                    //return processData(response.data, fields);
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
    return dataProcessingService;
}]);


gsnMap.factory('MapFilterParameters', [function () {
    function MapFilterParameters() {

        this.sensors = [];
        this.sensorName = {};
        this.group = {};
        this.deployment = '';
        this.parameters = [];
        this.onlyPublic = true;
        this.fromDate = {};
        this.untilDate = {};

    }

    return new MapFilterParameters();
}]);