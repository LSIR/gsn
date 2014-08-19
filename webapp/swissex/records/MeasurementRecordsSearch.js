
var data = null;
var mapConfig = null;

function ConvertDMSToDD(days, minutes, seconds, direction) {
    var dd = parseFloat(days);

    if (isNaN(minutes) == false) {
        dd += parseFloat(minutes) / 60;
        if (isNaN(seconds) == false) {
            dd += parseFloat(seconds) / (60 * 60);
        } else direction = seconds;
    } else direction = minutes;

    if (direction == "S" || direction == "W") {
        dd = dd * -1;
    } // Don't do anything for N or E
    return dd;
}

var infowindow = new google.maps.InfoWindow({
    content: ""
});

function bindInfoWindow(marker, map, infowindow, strDescription) {
    google.maps.event.addListener(marker, 'click', function () {
        infowindow.setContent(strDescription);
        infowindow.open(map, marker);
    });
}

function ConvertDMSToDD(days, minutes, seconds, direction) {
    var dd = parseFloat(days);

    if (isNaN(minutes) == false) {
        dd += parseFloat(minutes) / 60;
        if (isNaN(seconds) == false) {
            dd += parseFloat(seconds) / (60 * 60);
        } else direction = seconds;
    } else direction = minutes;

    if (direction == "S" || direction == "W") {
        dd = dd * -1;
    } // Don't do anything for N or E
    return dd;
}

function parseCoordinates(coordinatesString) {
    var parts = coordinatesString.split(/[^\d\w\.]+/);
    var lat = 0, lon = 0;
    if (parts.length > 2) {
        lat = ConvertDMSToDD(parts[0], parts[1], parts[2], parts[3]);
        for (j = 0; j < parts.length;) if (isNaN(parts[j]) == true) break; else j++;
        lon = ConvertDMSToDD(parts[j + 1], parts[j + 2], parts[j + 3], parts[j + 4]);
    } else {
        lat = parseFloat(parts[0]);
        lon = parseFloat(parts[1]);
    }

    return new google.maps.LatLng(lat, lon);
}

function getLink(dataEntry) {
    var link = "http://www.swiss-experiment.ch/index.php/" + dataEntry['title'];
    while (link.indexOf(' ') > -1) {
        link = link.replace(" ", "%20");
    }
    return link;
}

function getDeploymentName(dataEntry) {
    var depl = "";

    if (typeof dataEntry['DeploymentName'] == 'undefined') {
        if (typeof dataEntry['Deployment'] == 'undefined') {
            dataEntry['DeploymentName'] = 'undefined';
            return depl;
        } else {
            dataEntry['DeploymentName'] = dataEntry['Deployment'];
        }
    }

    depl = dataEntry['DeploymentName'];
    return depl;
}

function makeInfoWinContentForOneLoc(dataEntry) {
    var contentString = "";

    var cs_link = getLink(dataEntry);
    var cs_depl = getDeploymentName(dataEntry);
    var cs_loc = "";
    if (typeof dataEntry['Location'] != 'undefined') cs_loc = dataEntry['Location'];
    var cs_cat = "";
    if (typeof dataEntry['Category'] != 'undefined') cs_cat = dataEntry['Category'];
    var cs_exp = "";
    if (typeof dataEntry['experiments'] != 'undefined') cs_exp = dataEntry['experiments'];

    contentString = 'Link: <a href=' + cs_link + '>' + cs_link + '</a></br>Deployment/Fieldsite: ' + cs_depl + '</br> Location: ' + cs_loc + '</br> Category: ' + cs_cat + '</br> Experiment: ' + cs_exp;

    return contentString;
}


var filtersLoc = [];
filtersLoc[0] = new FilterDeploymentName("#link_deployment", ".filter_deployment");
filtersLoc[1] = new FilterAltitude("#link_altitude", ".filter_altitude");
filtersLoc[2] = new FilterExperiment("#link_experiment", ".filter_experiment");

var filtersRec = [];
filtersRec[0] = new FilterRecord("MeasurementMedia", ".medium", "fltr_medm_checkbox_", "#search_med", "#clear_all_med", "#check_all_med", "#link_medium", ".filter_medium");
filtersRec[1] = new FilterRecord("MeasuredParameter", ".quantity", "fltr_qntn_checkbox_", "#search_quantity", "#clear_all_q", "#check_all_q", "#link_quantity", ".filter_quantity");
filtersRec[2] = new FilterRecord("Location", ".location", "fltr_loct_checkbox_", "#search_loc", "#clear_all_loc", "#check_all_loc", "#link_location", ".filter_location");
filtersRec[3] = new FilterDeployment("#link_deployed", ".filter_deployed");

var infoWindowContent = [];
var infoWindowContentRecords = [];

var cannotShowOnMapLoc = [];
var cannotShowOnMapCntLoc = 0;
var cannotShowOnMapRec = [];
var cannotShowOnMapCntRec = 0;
var coordToMarkerind = [];//lat to lon to ind



function initialize() {
    var mapOptions = {
        center: new google.maps.LatLng(mapConfig['lat'], mapConfig['lng']),
        zoom: mapConfig['zoom']
    };

    map = new google.maps.Map(document.getElementById("map-canvas"), mapOptions);

    for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {

        if (typeof data[category_loc][i]['Coordinates'] == 'undefined') {
            cannotShowOnMapLoc[cannotShowOnMapCntLoc++] = data[category_loc][i];
            continue;
        }

        var loc = data[category_loc][i]['Location'].trim().toLowerCase();
        data[category_loc][i]['Location'] = loc;
        if (typeof locationToInd[loc] == 'undefined') {
            locationToInd[loc] = [];
            locationToInd[loc]['ind'] = [];
            locationToInd[loc]['ind'][0] = i;
            locationToIndCnt[loc] = 1;
        } else {
            locationToInd[loc]['ind'][locationToIndCnt[loc]++] = i;
        }
        locationToInd[loc][locationToIndCnt[loc] - 1] = [];
        locationToInd[loc][locationToIndCnt[loc] - 1]['hasrecord'] = false;

        var latlng = parseCoordinates(data[category_loc][i]['Coordinates']);
        markers[i] = new google.maps.Marker({
            position: latlng,
            map: map,
            title: data[category_loc][i]['title']
        });

        var lat = latlng.lat();
        var lon = latlng.lng();
        data[category_loc][i]['lat'] = latlng.lat();
        data[category_loc][i]['lon'] = latlng.lng();

        markersCond[i] = [];

        var contentString = "";

        if (typeof coordToMarkerind[lat] == 'undefined') {
            coordToMarkerind[lat] = [];
        }
        if (typeof coordToMarkerind[lat][lon] == 'undefined') {
            coordToMarkerind[lat][lon] = [];
            coordToMarkerind[lat][lon][0] = i;
            coordToMarkerind[lat][lon]['cnt'] = 1;

            contentString = makeInfoWinContentForOneLoc(data[category_loc][i]);

        } else {
            //multiple markers with same coordinates
            coordToMarkerind[lat][lon][coordToMarkerind[lat][lon]['cnt']++] = i;

            if (coordToMarkerind[lat][lon]['cnt'] == 2) {
                var prev_link = getLink(data[category_loc][coordToMarkerind[lat][lon][0]]);
                var prev_title = data[category_loc][coordToMarkerind[lat][lon][0]]['title'];
                coordToMarkerind[lat][lon]['contentString'] = 'Locations:<li><a href=' + prev_link + '>' + prev_title + '</a></li>';
                coordToMarkerind[lat][lon]['contentString'] += '<li><a href=' + getLink(data[category_loc][i]) + '>' + data[category_loc][i]["title"] + '</a></li>';

                contentString = coordToMarkerind[lat][lon]['contentString'];
            }
            else {
                coordToMarkerind[lat][lon]['contentString'] += '<li><a href=' + getLink(data[category_loc][i]) + '>' + data[category_loc][i]["title"] + '</a></li>';

                contentString = coordToMarkerind[lat][lon]['contentString'];
            }

            for (var k = 0; k < coordToMarkerind[lat][lon]['cnt'] - 1; k++) {
                google.maps.event.clearInstanceListeners(markers[coordToMarkerind[lat][lon][k]]);
                infoWindowContent[coordToMarkerind[lat][lon][k]] = contentString;
            }
        }

        infoWindowContent[i] = contentString;

    }

    for (var i = 0; i < Object.keys(data[category_rec]).length; i++) {
        var loc = data[category_rec][i]['Location'];
        if (typeof loc == 'undefined') {
            cannotShowOnMapRec[cannotShowOnMapCntRec++] = data[category_rec][i];
            continue;
        }

        loc = loc.trim().toLowerCase();
        data[category_rec][i]['Location'] = loc;

        if (typeof locationToInd[loc] == 'undefined') {
            cannotShowOnMapRec[cannotShowOnMapCntRec++] = data[category_rec][i];
            continue;
        }
        locationToInd[loc][locationToIndCnt[loc] - 1]['hasrecord'] = true;

        //filtersRec[0].addData(data[category_rec][i], locationToInd[loc]['ind']);//only add data for mediums

        var link = getLink(data[category_rec][i]);

        for (var k = 0; k < locationToInd[loc]['ind'].length; k++) {
            var lat = data[category_loc][locationToInd[loc]['ind'][k]]['lat'];
            var lon = data[category_loc][locationToInd[loc]['ind'][k]]['lon'];
            for (var p = 0; p < coordToMarkerind[lat][lon]['cnt']; p++) {
                if (typeof infoWindowContentRecords[coordToMarkerind[lat][lon][p]] == 'undefined') {
                    infoWindowContentRecords[coordToMarkerind[lat][lon][p]] = "";
                }
                infoWindowContentRecords[coordToMarkerind[lat][lon][p]] += '<li><a href="' + link + '">' + data[category_rec][i]['title'] + '</a></li>';
            }
        }
    }

    for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
        if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
        infoWindowContent[i] += "</br>Records:";
        if (typeof infoWindowContentRecords[i] != 'undefined') infoWindowContent[i] += infoWindowContentRecords[i];
        bindInfoWindow(markers[i], map, infowindow, infoWindowContent[i]);
    }

    function initLocationFilter(initialized, filter) {
        var toRetInitialized = initialized;
        if (initialized == false) {
            toRetInitialized = true;
            for (var i = 0; i < Object.keys(data[category_loc]).length; i++) {
                if (typeof data[category_loc][i]['Coordinates'] == 'undefined') continue;
                filter.addData(data[category_loc][i], i);

            }
            filter.initialize(map, markers, data);
        }
        return toRetInitialized;
    }

    var initializedFlags = [];
    for (var i = 0; i < filtersLoc.length + filtersRec.length; i++) {
        initializedFlags[i] = false;
    }

    function initRecordFilter(initialized, filter) {
        var toRetInitialized = initialized;
        if (initialized == false) {
            toRetInitialized = true;
            for (var i = 0; i < Object.keys(data[category_rec]).length; i++) {
                var loc = data[category_rec][i]['Location'];
                if (typeof loc == 'undefined') continue;
                if (typeof locationToInd[loc] == 'undefined') continue;
                filter.addData(data[category_rec][i], locationToInd[loc]['ind']);
            }
            filter.initialize(map, markers, data, locationToInd, locationToIndCnt);
        }
        return toRetInitialized;
    }

    function addLinkListener(linkID, filerDivID, filter, location, ind) {
        $(linkID).click(function () {
            if ($(filerDivID).css("display") == "none") {
                if (location == true) {
                    initializedFlags[ind] = initLocationFilter(initializedFlags[ind], filter);
                } else {
                    initializedFlags[ind] = initRecordFilter(initializedFlags[ind], filter);
                }
            }
            $(filerDivID).toggle();
        });
    }


    for (var i = 0; i < filtersLoc.length; i++) {
        addLinkListener(filtersLoc[i].getLinkID(), filtersLoc[i].getFilterDivID(), filtersLoc[i], true, i)
    }
    for (var i = 0; i < filtersRec.length; i++) {
        addLinkListener(filtersRec[i].getLinkID(), filtersRec[i].getFilterDivID(), filtersRec[i], false, i + filtersLoc.length)
    }

    for (var i = 0; i < filtersLoc.length + filtersRec.length; i++) {
        for (var j = 0; j < markers.length; j++) {
            if (typeof markers[j] != 'undefined') markersCond[j][i] = true;
        }
    }

    $("#cannotshow").html("<h4>" + cannotShowOnMapCntRec + " out of " + Object.keys(data[category_rec]).length + " Measurement Records cannot be plotted.</br>" + cannotShowOnMapCntLoc + " out of " +
        Object.keys(data[category_loc]).length + " Measurement Locations cannot be plotted.</h4>");

}

function initializeWrapper (){
    $.get('data/MLocRec.json', "", function(response, text_status, request){
        data = JSON.parse(response);
        $.get('MapConfig.json', "", function(response, text_status, request){
            mapConfig = JSON.parse(response);
            initialize();
        });
    });

}

google.maps.event.addDomListener(window, 'load', initializeWrapper);
