var gsnMap = angular.module("gsnMap", ["leaflet-directive"]);

gsnMap.controller("GoogleMapsController", ["$scope", 'leafletData', '$compile', '$filter', 'sensors', 'FilterParameters', '$location', '_', 'MapFilterParameters',
    function ($scope, leafletData, $compile, $filter, sensors, FilterParameters, $location, _, MapFilterParameters) {


        $scope.geojson = {};
        $scope.data = sensors.data;
        $scope.geojson.data = sensors.data;
        $scope.features = sensors.data.features;


        $scope.center = {
            lat: 46.4,
            lng: 8,
            zoom: 8
        };

        //----------------------------
        //Initialize filter parmeters
        //----------------------------

        var namesOfGroup = {};
        var parametersOfGroup = {};

        var namesOfGroupPublic = {};
        var parametersOfGroupPublic = {};
        var publicSensors = [];

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
                publicSensors.push(properties.sensorName);
            }
        }

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

        setFilterParameters();
        $scope.currentMarkers = L.markerClusterGroup();

        updateMarkers();

        function updateFilterParameters(namesOfGroup, parametersOfGroup) {
            $scope.groups = ['All'].concat(_.keys(namesOfGroup).sort());
            $scope.mapSensorNames = [].concat.apply([], _.values(namesOfGroup).sort());
            $scope.parameters = _.uniq([].concat.apply([], _.values(parametersOfGroup)).sort(), true);
        }


        $scope.updateGroup = function (item) {
            if (item === 'All') {
                $scope.mapSensorNames = [].concat.apply([], _.values(namesOfGroup).sort());
                $scope.parameters = _.uniq([].concat.apply([], _.values(parametersOfGroup)).sort(), true);
            } else {
                $scope.mapSensorNames = [].concat(namesOfGroup[item].sort());
                $scope.parameters = _.uniq([].concat(parametersOfGroup[item].sort()), true);

            }
            $scope.filter.sensors = [];
            updateMarkers();
        };

        $scope.changePrivacy = function () {
            setFilterParameters();
            updateMarkers();
        };

        function setFilterParameters() {
            if ($scope.filter.onlyPublic) {
                updateFilterParameters(namesOfGroupPublic, parametersOfGroupPublic);
            } else {
                updateFilterParameters(namesOfGroup, parametersOfGroup);
            }

        }


        $scope.submit = function () {
            updateMarkers();
        };

        $scope.getSensorIcon = function (sensorName) {
            //if (_.contains(publicSensors, sensorName)) {
            if (publicSensors.indexOf(sensorName) > -1) {
                return 'img/green_.png';
            } else {
                return 'img/red_.png';
            }
        };


        function updateMarkers() {
            //var markers = L.markerClusterGroup();

            $scope.filter.updateUrl();

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


            $scope.sensorsCount = 0;

            var geoJsonLayer = L.geoJson(sensors.data, {
                onEachFeature: onEachFeature,

                filter: function (feature, layer) {
                    var included = filterSensor(feature);
                    if (included) {
                        $scope.sensorsCount ++;
                    }
                    return included;
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
                if ($scope.sensorsCount > 0) {
                    map.fitBounds($scope.currentMarkers.getBounds());
                }
            });

        }


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

            result = result && (feature.properties.elevation >= $scope.filter.altitude.min) &&
            (feature.properties.elevation <= $scope.filter.altitude.max);

            result = result && (feature.properties.aspect >= $scope.filter.aspect.min) &&
            (feature.properties.aspect <= $scope.filter.aspect.max);

            result = result && (feature.properties.slopeAngle >= $scope.filter.slopeAngle.min) &&
            (feature.properties.slopeAngle <= $scope.filter.slopeAngle.max);

            var from = new Date(feature.properties.fromDate);
            from.setHours(0, 0, 0, 0);
            var to = new Date(feature.properties.toDate);
            to.setHours(0, 0, 0, 0);

            if (notEmptyDate($scope.filter.fromDate)) {
                result = result && ($scope.filter.fromDate.valueOf() >= from.valueOf())
                && ($scope.filter.fromDate.valueOf() <= to.valueOf());
            }

            if (notEmptyDate($scope.filter.untilDate)) {
                result = result && ($scope.filter.untilDate.valueOf() >= from.valueOf())
                && ($scope.filter.untilDate.valueOf() <= to.valueOf());
            }
            return result;
        }

        $scope.$on("slideEnded", function () {
            updateMarkers();
        });

        $scope.$watch('filter.fromDate', function (newVal, oldVal) {
            if (newVal && oldVal && newVal.toString() != oldVal.toString()) {
                console.log('From date' + $scope.filter.fromDate);
                updateMarkers();
            }
        });

        $scope.$watch('filter.untilDate', function (newVal, oldVal) {
            if (newVal && oldVal && newVal.toString() != oldVal.toString()) {
                console.log('Until date' + $scope.filter.untilDate);
                updateMarkers();
            }
        });

        function notEmptyDate(date) {
            return (!isNaN(date) && date && date.valueOf() > 0);
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

            //var html = '<div><b>{{sensorName}}</b></br><i>has data from {{fromDate}} to {{toDate}}</i><br><Label>Parameters</Label><ul><li ng-repeat="param in parameters">{{param}}</li></ul><br><md-button ng-disabled="protected" class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';
            var html = "";
            jQuery.get("partials/sensor_window.html", function (data) {
                html = data;
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
                newScope.parameterString = feature.properties.observed_properties.join(', ');
                newScope.elevation = feature.properties.elevation;
                newScope.angle = feature.properties.slopeAngle;
                newScope.aspect = feature.properties.aspect;

                var linkFunction = $compile(html)(newScope);

                layer.bindPopup(linkFunction[0]);
            });

            //'<div><b>{{sensorName}}</b></br><i>has data from {{fromDate}} to {{toDate}}</i><br/><b>Parameters: </b>{{parameterString}}<br/><ul><li><b>Elevation:</b>{{elevation}}</li>' +
            //    '<li><b>Slope angle:</b>{{angle}}</li><li><b>Aspect:</b>{{aspect}}</li></ul><md-button ng-disabled="protected" class="md-raised md-primary" ng-click="plot(feature);">Plot</md-button></div>';

            //var html = '<div><b>{{extra}}</b><p>Parameters</p><table><tr ng-repeat="param in parameters"><td>{{param.name}}</td></tr></table><ul><li ng-repeat="param in parameters">{{param}}</li></ul><br><md-button class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';


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

        };

        $scope.monitor = function (feature) {
            console.log('MONITOR ' + feature.properties.sensorName);
            $location.path('/monitor')
            $location.search('sensors', [feature.properties.sensorName].toString());

        };

        function getParametersForLink(feature) {
            var colNames = [];
            for (var i = 0; i < $scope.filter.parameters.length; i++) {
                colNames = _.union(colNames, feature.properties[$scope.filter.parameters[i]])
            }
            return colNames;
        }


    }

]);


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
    this.promise;

    var self = this;
    var sdo = {
        getSensors: function () {

            if (!self.promise) {
                self.promise = $http({
                    method: 'GET',
                    url: 'http://montblanc.slf.ch:8090/web/virtualSensors?onlyPublic=false'
                    //url: 'http://eflumpc18.epfl.ch/gsn/web/virtualSensors?onlyPublic=false'
                    //url: 'http://localhost:8090/web/virtualSensors?onlyPublic=false'
                });
                self.promise.success(function (data, status, headers, conf) {
                    return data;
                });
            }
            return self.promise;
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


gsnMap.factory('MapFilterParameters', ['$location', '$filter', '$route',
    function ($location, $filter, $route) {

        function MapFilterParameters() {

            if ($route.current.params.sensors)
                this.sensors = $route.current.params.sensors.split(',');
            else this.sensors = [];

            this.group = {};
            if ($route.current.params.group)
                this.group.selected = $route.current.params.group;


            if ($route.current.params.parameters)
                this.parameters = $route.current.params.parameters.split(',');
            else this.parameters = [];

            this.onlyPublic = false;
            if ($route.current.params.onlyPublic)
                this.onlyPublic = ($route.current.params.onlyPublic === 'true');


            this.fromDate = new Date($route.current.params.from);
            this.fromDate.setHours(0, 0, 0, 0);
            this.untilDate = new Date($route.current.params.to);
            this.untilDate.setHours(0, 0, 0, 0);

            this.altitude = {
                min: 0,
                max: 4700,
                floor: 0,
                ceil: 4700
            };
            this.slopeAngle = {
                min: 0,
                max: 90,
                floor: 0,
                ceil: 90
            };

            this.aspect = {
                min: 0,
                max: 360,
                floor: 0,
                ceil: 360
            }

        }

        MapFilterParameters.prototype = {

            updateUrl: function () {
                console.log('UPDATE map filters');
                $location.path('/map', false);

                if (this.sensors.length > 0) {
                    $location.search('sensors', this.sensors.toString());
                } else {
                    $location.search('sensors', null);

                }

                if (this.parameters.length > 0) {
                    $location.search('parameters', this.parameters.toString());
                } else {
                    $location.search('parameters', null);
                }

                $location.search('group', this.group.selected);
                $location.search('onlyPublic', this.onlyPublic.toString());

                if (this.getDate(this.fromDate)) {
                    $location.search('from', this.formatDateWeb(this.getDate(this.fromDate)));
                } else {
                    $location.search('from', null);
                }

                if (this.getDate(this.untilDate)) {
                    $location.search('to', this.formatDateWeb(this.getDate(this.untilDate)));
                } else {
                    $location.search('to', null);
                }

            },

            formatDateWeb: function (date) {
                return $filter('date')(Date.parse(date), 'yyyy-MM-dd');
            },

            getDate: function (date) {
                if (isNaN(date)) return null;

                if (typeof(date) == 'number') {
                    return new Date(date);
                }
                return date;
            },


            hasDates: function () {
                return this.untilDate && this.fromDate;
            }

        };
        return new MapFilterParameters();
    }]);