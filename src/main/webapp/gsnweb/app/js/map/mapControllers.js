var gsnMap = angular.module("gsnMap", ["leaflet-directive"]);


gsnMap.controller("GoogleMapsController", ["$scope", '$http', 'leafletData', '$compile', '$filter', 'sensors', 'FilterParameters', 'sharedService', '$location', '_',
    function ($scope, $http, leafletData, $compile, $filter, sensors, FilterParameters, sharedService, $location, _) {


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

        for (var i = 0; i < $scope.features.length; i++) {
            var properties = $scope.features[i].properties;
            if (!(namesOfGroup[properties.group])) {
                namesOfGroup[properties.group] = [];
                parametersOfGroup[properties.group] = [];
            }
            namesOfGroup[properties.group].push(properties.sensorName);
            parametersOfGroup[properties.group] = _.union(parametersOfGroup[properties.group], properties.observed_properties);

            //parametersOfGroup[properties.group].push.apply(parametersOfGroup[properties.group], properties.observed_properties);
        }

        $scope.groups = ['All'].concat(_.keys(namesOfGroup).sort());
        $scope.sensorNames = [].concat.apply([], _.values(namesOfGroup).sort());
        $scope.parameters = _.uniq([].concat.apply([], _.values(parametersOfGroup)).sort(), true);


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

        $scope.group = {};

        $scope.filter = {
            sensors: [],
            sensorName: {},
            group: {},
            deployment: '',
            parameters: [],
            sensorNameFeature: {}
        };

        $scope.submit = function() {
            updateMarkers();
        };

        $scope.currentMarkers = L.markerClusterGroup();


        function updateMarkers() {
            //var markers = L.markerClusterGroup();

            var geoJsonLayer = L.geoJson(sensors.data, {
                onEachFeature: onEachFeature,
                filter: function (feature, layer) {
                    return filterSensor(feature);
                }
            });


            $scope.currentMarkers.clearLayers();
            $scope.currentMarkers.addLayer(geoJsonLayer);
            leafletData.getMap().then(function (map) {
                //map.removeLayer($scope.currentMarkers);
                map.addLayer( $scope.currentMarkers);
                map.fitBounds( $scope.currentMarkers.getBounds());
            });

        }

        updateMarkers();

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

            return result;
        }


        function onEachFeature(feature, layer) {
            var sensorName = feature.properties.sensorName;

            //var html = '<div><b>{{sensorName}}</b><br><a href="#/plot?sensors={{sensorName}}&parameters={{parameters}}" my-refresh>Plot</a></div>';
            var html = '<div><b>{{sensorName}}</b><p>Parameters</p><ul><li ng-repeat="param in parameters">{{param}}</li></ul><br><md-button class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';

            var newScope = $scope.$new();
            newScope.sensorName = sensorName;
            newScope.feature = feature;
            newScope.parameters = feature.properties.observed_properties;

            var linkFunction = $compile(html)(newScope);

            layer.bindPopup(linkFunction[0]);
        }

        $scope.plot = function(feature) {
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
        //$scope.centerJSON = function () {
        //    leafletData.getMap().then(function (map) {
        //        var latlngs = [];
        //        for (var i in $scope.geojson.data.features[0].geometry.coordinates) {
        //            var coord = $scope.geojson.data.features[0].geometry.coordinates[i];
        //            for (var j in coord) {
        //                var points = coord[j];
        //                for (var k in points) {
        //                    latlngs.push(L.GeoJSON.coordsToLatLng(points[k]));
        //                }
        //            }
        //        }
        //        map.fitBounds(latlngs);
        //    });
        //};

    }]);


gsnMap.directive('myRefresh',function($location,$route){
    return function(scope, element, attrs) {
        element.bind('click',function(){
            if(element[0] && element[0].href && element[0].href === $location.absUrl()){
                $route.reload();
            }
        });
    }
});

gsnMap.controller('AppCtrl', function ($scope, $timeout, $mdSidenav, $mdUtil, $log) {
    $scope.toggleLeft = buildToggler('left');
    $scope.toggleRight = buildToggler('right');
    /**
     * Build handler to open/close a SideNav; when animation finishes
     * report completion in console
     */
    function buildToggler(navID) {
        var debounceFn =  $mdUtil.debounce(function(){
            $mdSidenav(navID)
                .toggle()
                .then(function () {
                    $log.debug("toggle " + navID + " is done");
                });
        },300);
        return debounceFn;
    }
});
//gsnMap.controller('LeftCtrl', function ($scope, $timeout, $mdSidenav, $log) {
        //$scope.close = function () {
        //    $mdSidenav('left').close()
        //        .then(function () {
        //            $log.debug("close LEFT is done");
        //        });
        //};
//    });
//gsnMap.controller('RightCtrl', function ($scope, $timeout, $mdSidenav, $log) {
//        $scope.close = function () {
//            $mdSidenav('right').close()
//                .then(function () {
//                    $log.debug("close RIGHT is done");
//                });
//        };
//    });

gsnMap.factory('Sensors', ['$http', function($http) {
    var sdo = {
        getSensors: function() {
            var promise = $http({
                method: 'GET',
                //url: 'http://eflumpc18.epfl.ch/gsn/metadata/virtualSensors'
                url: 'http://localhost:8090/web/virtualSensors'
            });
            promise.success(function(data, status, headers, conf) {
                return data;
            });
            return promise;
        }
    };
    return sdo;
}]);