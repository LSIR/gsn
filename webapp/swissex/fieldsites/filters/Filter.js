function AbstractFilter () {
}

var category = "Fieldsites";

var map;
var markers = [];
var markerCluster = null;
var markersCond = [];
var numOfFilters = 0;

function stateChanged(i){
    for (var j = 0; j< numOfFilters; j++){
        if (markersCond[i][j] == false){
            markers[i].setMap(null);
            //markerCluster.removeMarker(markers[i]);
            return;
        }
    }
    markers[i].setMap(map);
    //markerCluster.addMarker(markers[i]);
}


AbstractFilter.prototype.initialize = function(map, markers, data) { };
