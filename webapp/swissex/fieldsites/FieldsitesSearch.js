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

//filters
var filtersLoc = [];
filtersLoc[0] = new FilterDeploymentName();
filtersLoc[1] = new FilterAltitude();


var cannotShowOnMapLoc = [];
var cannotShowOnMapCntLoc = 0;
var coordToMarkerind = [];//lat to lon to ind

function initialize() {
    var mapOptions = {
        center: new google.maps.LatLng(mapConfig['lat'], mapConfig['lng']),
        zoom: mapConfig['zoom']
    };
    
    map = new google.maps.Map(document.getElementById("map-canvas"), mapOptions);
    //markerCluster = new MarkerClusterer(map, []);
    
    for (var i = 0; i < Object.keys(data[category]).length; i++) {

        if (typeof data[category][i]['Coordinates'] == 'undefined') {
            cannotShowOnMapLoc[cannotShowOnMapCntLoc++] = data[category][i];
            continue;
        }

        var parts = data[category][i]['Coordinates'].split(/[^\d\w\.]+/);
        var lat = 0, lon = 0;
        if (parts.length > 2) {
            lat = ConvertDMSToDD(parts[0], parts[1], parts[2], parts[3]);
            for (j = 0; j < parts.length;) if (isNaN(parts[j]) == true) break; else j++;
            lon = ConvertDMSToDD(parts[j + 1], parts[j + 2], parts[j + 3], parts[j + 4]);
        } else {
            lat = parseFloat(parts[0]);
            lon = parseFloat(parts[1]);
        }

        markers[i] = new google.maps.Marker({
            position: new google.maps.LatLng(lat, lon),
            map: map,
            title: data[category][i]['title']
        });
        markersCond[i] = [];
        //markerCluster.addMarker(markers[i]);


        for (var k = 0; k < filtersLoc.length; k++){
            filtersLoc[k].addData(data[category][i], i);
        }

        var contentString = "";
        var cs_link = "http://www.swiss-experiment.ch/index.php/" + data[category][i]['title'];
        while (cs_link.indexOf(' ') > -1) {
            cs_link = cs_link.replace(" ", "%20");
        }
        var cs_depl = data[category][i]['Deployment'];

        if (typeof coordToMarkerind[lat] == 'undefined') {
            coordToMarkerind[lat] = [];
        }
        if (typeof coordToMarkerind[lat][lon] == 'undefined') {
            coordToMarkerind[lat][lon] = [];
            coordToMarkerind[lat][lon][0] = i;
            coordToMarkerind[lat][lon]['cnt'] = 1;

            var cs_loc = "";
            if (typeof data[category][i]['Location'] != 'undefined') cs_depl = data[category][i]['Location'];
            var cs_cat = "";
            if (typeof data[category][i]['Category'] != 'undefined') cs_depl = data[category][i]['Category'];
            var cs_exp = "";
            if (typeof data[category][i]['experiments'] != 'undefined') cs_depl = data[category][i]['experiments'];
            contentString = 'Link: <a href=' + cs_link + '>' + cs_link + '</a></br>Deployment/Fieldsite: ' + cs_depl + '</br> Location: ' + cs_loc + '</br> Category: ' + cs_cat + '</br> Experiment: ' + cs_exp;

        } else {
            //multiple markers with same coordinates
            coordToMarkerind[lat][lon][coordToMarkerind[lat][lon]['cnt']++] = i;

            if (coordToMarkerind[lat][lon]['cnt'] == 2) {
                var prev_link = "http://www.swiss-experiment.ch/index.php/" + data[category][coordToMarkerind[lat][lon][0]]['title'];
                while (prev_link.indexOf(' ') > -1) {
                    prev_link = prev_link.replace(" ", "%20");
                }
                var prev_depl = data[category][coordToMarkerind[lat][lon][0]]['title'];
                coordToMarkerind[lat][lon]['contentString'] = '<li><a href=' + prev_link + '>' + prev_depl + '</a></li>';
                coordToMarkerind[lat][lon]['contentString'] += '<li><a href=' + cs_link + '>' + data[category][i]["title"] + '</a></li>';
                contentString = coordToMarkerind[lat][lon]['contentString'];
            }
            else {
                coordToMarkerind[lat][lon]['contentString'] += '<li><a href=' + cs_link + '>' + data[category][i]["title"] + '</a></li>';

                contentString = coordToMarkerind[lat][lon]['contentString'];

            }

            //add info windows
            for (var k = 0; k < coordToMarkerind[lat][lon]['cnt'] - 1; k++) {
                google.maps.event.clearInstanceListeners(markers[coordToMarkerind[lat][lon][k]]);
                bindInfoWindow(markers[coordToMarkerind[lat][lon][k]], map, infowindow, contentString);
            }
        }


        bindInfoWindow(markers[i], map, infowindow, contentString);

    }

    $( "#link_deployment").click(function(){
        $( ".filter_deployment" ).toggle();
    });

    $( "#link_altitude").click(function(){
        $( ".filter_altitude" ).toggle();
    });

    //Filters
    for (var i = 0; i < filtersLoc.length; i++) {
        filtersLoc[i].initialize(map, markers, data);
    }
    for (var i = 0; i < filtersLoc.length; i++) {
        for (var j = 0; j < markers.length; j++){
            if (typeof markers[j] != 'undefined') markersCond[j][i] = true;
        }
    }

    if (cannotShowOnMapCntLoc > 0){
        $("#cannotshow").html("<h4>" + cannotShowOnMapCntLoc + " out of " + Object.keys(data[category]).length + " Fieldsites cannot be plotted.</h4>");
    }
}

function initializeWrapper (){
    $.get('data/Fieldsites.json', "", function(response, text_status, request){
        data = JSON.parse(response);
        $.get('MapConfig.json', "", function(response, text_status, request){
            mapConfig = JSON.parse(response);
            initialize();
        });
    });

}

google.maps.event.addDomListener(window, 'load', initializeWrapper);