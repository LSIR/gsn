var gsnMap = angular.module("gsnMap", ["leaflet-directive"]);


gsnMap.controller("GoogleMapsController", ["$scope", '$http', 'leafletData', '$compile', '$filter', 'sensors', 'FilterParameters', 'sharedService', '$location',
    function ($scope, $http, leafletData, $compile, $filter, sensors, FilterParameters, sharedService, $location) {

        $scope.person = {};
        $scope.people = [
            { name: 'Adam',      email: 'adam@email.com',      age: 10 },
            { name: 'Amalie',    email: 'amalie@email.com',    age: 12 },
            { name: 'Wladimir',  email: 'wladimir@email.com',  age: 30 },
            { name: 'Samantha',  email: 'samantha@email.com',  age: 31 },
            { name: 'Estefanía', email: 'estefanía@email.com', age: 16 },
            { name: 'Natasha',   email: 'natasha@email.com',   age: 54 },
            { name: 'Nicole',    email: 'nicole@email.com',    age: 43 },
            { name: 'Adrian',    email: 'adrian@email.com',    age: 21 }
        ];



        $scope.geojson = {};
        $scope.data = sensors.data;
        $scope.geojson.data = sensors.data;
        $scope.features = sensors.data.features;


        $scope.center = {
            lat: 46.4,
            lng: 8,
            zoom: 8
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

        $scope.filter = {
            sensorName: '',
            deployment: '',
            parameters: '',
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
            if ($scope.filter.sensorName.length > 0) {
                result = result && (feature.properties.sensorName === $scope.filter.sensorName);
            }
            if ($scope.filter.deployment.length > 0) {
                result = result && (feature.properties.deployment === $scope.filter.deployment);
            }
            if ($scope.filter.parameters.length > 0) {
                result = result && (feature.properties.observed_properties.indexOf($scope.filter.parameters) > -1);
            }
            if ($scope.filter.sensorNameFeature.properties) {
                result = result && (feature.properties.sensorName === $scope.filter.sensorNameFeature.properties.sensorName);
            }

            return result;
        }

        //angular.extend($scope, {
        //    geojson: {
        //        data: $scope.geojson.data,
        //        style: function (feature) {
        //            return {};
        //        }
        //    }
        //});

        //addGeoJsonLayerWithClustering($scope.geojson.data);

        //function addGeoJsonLayerWithClustering(data) {

        //}

        //var markers = L.markerClusterGroup();
        //
        //for (var i = 0; i < sensors.length; i++) {
        //    var a = sensors[i];
        //    var title = a.properties.sensorName;
        //    var marker = L.marker(new L.LatLng(a.geometry.coordinates[0], a.geometry.coordinates[1]), { title: title });
        //    marker.bindPopup(title);
        //    markers.addLayer(marker);
        //}
        //
        //leafletData.getMap().then(function(map) {
        //    map.addLayer(markers);
        //    map.fitBounds(markers.getBounds());
        //});

        function onEachFeature(feature, layer) {
            var sensorName = feature.properties.sensorName;

            //var html = '<div><b>{{sensorName}}</b><br><a href="#/plot?sensors={{sensorName}}&parameters={{parameters}}" my-refresh>Plot</a></div>';
            var html = '<div><b>{{sensorName}}</b><br><md-button class="md-raised" ng-click="plot(feature);">Plot</md-button></div>';

            var newScope = $scope.$new();
            newScope.sensorName = sensorName;
            newScope.feature = feature;
            newScope.parameters = getParametersForLink(feature);

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
            if ($scope.filter.parameters.length >0) {
                return feature.properties[$scope.filter.parameters];
            }
            return [];
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
gsnMap.controller('LeftCtrl', function ($scope, $timeout, $mdSidenav, $log) {
        $scope.close = function () {
            $mdSidenav('left').close()
                .then(function () {
                    $log.debug("close LEFT is done");
                });
        };
    });
gsnMap.controller('RightCtrl', function ($scope, $timeout, $mdSidenav, $log) {
        $scope.close = function () {
            $mdSidenav('right').close()
                .then(function () {
                    $log.debug("close RIGHT is done");
                });
        };
    });

gsnMap.factory('Sensors', ['$http', function($http) {
    var sdo = {
        getSensors: function() {
            var promise = $http({
                method: 'GET',
                url: 'http://eflumpc18.epfl.ch/gsn/metadata/virtualSensors'
            });
            promise.success(function(data, status, headers, conf) {
                return data;
            });
            return promise;
        }
    };
    return sdo;
}]);