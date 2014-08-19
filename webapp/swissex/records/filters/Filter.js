function AbstractFilter (linkID, filterDivID) {
    this.linkID = linkID;
    this.filterDivID = filterDivID;
}

AbstractFilter.prototype.getLinkID = function (){
    return this.linkID;
}

AbstractFilter.prototype.getFilterDivID = function (){
    return this.filterDivID;
}

var category_loc = "Measurement%20Location";
var category_rec = "Measurement%20Record";

var map;
var markers = [];
var markersCond = [];
var locationToInd = [];
var locationToIndCnt = [];
var numOfFilters = 0;

function stateChanged(i){
    for (var j = 0; j< numOfFilters; j++){
        if (markersCond[i][j] == false){
            markers[i].setMap(null);
            return;
        }
    }
    markers[i].setMap(map);
}


AbstractFilter.prototype.initialize = function(map, markers, data) { };
