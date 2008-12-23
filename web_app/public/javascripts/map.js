
// The map currently in used, either set to Google or to SearchCh
var map = null;

// The currently applied sensor filter
var sensor_filter = null;

// Map type to use, possible values: "street", "hybird", "satelite",
// "earth" or "terrain"
var map_type = "terrain";

// The div that surrounds the map dia
var container;

var overlays = new Array();

// An instance of the selector which builds the div containing
// the map service selector
var selector;

var popUp;

// Virtual sensor addressing information
var vsa = null;

// Map default parameters
var width = 848;
var height = 700;
var center_lat = 46.82261;
var center_lng = 8.22876;
var zoom = 8;

// Tells whether the error message about missing coordinates has
// been shown. To avoid showing it again when switching map
var coordinatesMissingErrorShown = false;

// Delay used before updating the markers, is used since search.ch
// is buggy and the map needs time to be drawn before you can modify markers
var delay = 1000;

// Draws the map into to the div with id map_container and draws the sensors onto the map
// filtered with information in sensor_filter (if null, draw all sensors). The map service
// used is set by map_service (either "google" or "search_ch")
function init(map_service, map_container, sensor_filter) {
	// Make google the default map
	if (map_service == null)
		map_service = "search_ch";
	
	// Get the div which contains the map
	var div = document.getElementById(map_container);
	div.style.width = width + "px";
	div.style.height = height + "px";
	div.style.zIndex = "1";
	
	// Create a new div which will contain the map and
	// the map service selector
	container = document.createElement('div');
	container.id = "mapContainer"
	container.style.width = width + "px";
	container.style.height = height + "px";
	container.style.position ="absolute"
	div.appendChild(container);
	
	// Create the selector, it will be added on top of the map
	// when the map is loaded
	selector = new MapServiceSelector(map_service);
	popUp = new PopUp();

	sensor_filter = sensor_filter;

	setMapService(map_service);
	loadVsa();
}

// Switch the map service
function setMapService(map_service) {
	Map.unload();

	switch (map_service) {
		case "google":
			map = Google;
			break;
		case "search_ch":
			map = SearchCh;
			break;
		default:
			alert(map_service + "is an unknown map service provider");
	}
	Map.load();
	selector.update();
	
	// If vsa is loaded but map has no markers then the sensors need to be added
	if (vsa != null && map.markers == null)
		addSensorsToMap();
	
	// If vsa is loaded we can also apply the filter
	if (vsa != null)
		setSensorFilter(sensor_filter)
}

// Applies a sensor filter and redraws the sensors
function setSensorFilter(filter) {
	sensor_filter = filter;
	
	var ids = new Array();

	for (var s in vsa) {
		if (sensor_filter == null || sensor_filter[vsa[s]["deployment"]]) {
			if (sensor_filter == null || sensor_filter[vsa[s]["deployment"]][s]) {
				if (vsa[s]["latitude"] && vsa[s]["longitude"]) {
					tmp = dvos[vsa[s]["deployment"]][s];
					for (t in tmp) {
						if (sensor_filter == null || sensor_filter[vsa[s]["deployment"]][s][t]) {
							ids[vsa[s]["id"]] = s;
							break;
						}
					}
				}
			}
		}
	}
	setTimeout(function() {
		map.hideMarkers();
		for (i in ids)
			map.showMarker(vsa[ids[i]]["id"]);
	},delay);
}

// Load all the sensor addressing information and
// then add all the sensors to the map
function loadVsa() {
	// Show loading message
	popUp.show(POPUP_IMAGE_LOADING, "Please wait", "loading sensor information...", false);
	
	// Is called when all the addressing information is recieved
	var vsaLoaded = function () {
		popUp.off();
		addSensorsToMap();
		setSensorFilter(sensor_filter);
	};
	
	// Use an ajax call with timeout 10s to retrieve sensor information
	$.ajax({url:"/map/vsa2", dataType:'json', timeout: 10000, success:
	    function(data){
			vsa = data;
			// Make sure that the sensor array is loaded. If not wait 
			// 2s and try again.
			if (dvos == null) {
				popUp.show(POPUP_IMAGE_LOADING, "Please wait", "starting 3s timeout", false);
				setTimeout(function() {
					if (dvos != null)
						vsaLoaded();
					else {
						popUp.show(POPUP_IMAGE_LOADING, "Please wait", "starting 7s timeout", false);
						// If failure after 3s wait 7s and if failure
						// after this time give an error message.
						setTimeout(function() {
							if (dvos != null)
								vsaLoaded();
							else
								popUp.show(POPUP_IMAGE_ERROR, "Error", "Unable to retrieve sensor information.<br>Try to reload this page...", false);
						},7000);
					}
				},3000);
			}
			else
				vsaLoaded();
	    }
	  });

	// A timeout set to 11s. If addressing information is not loaded by then give an error mesage.
	setTimeout(function() {
		if (vsa == null)
			popUp.show(POPUP_IMAGE_ERROR, "Error", "Unable to retrieve sensor information.<br>Try to reload this page...", false);
	},11000);
}

function addSensorsToMap() {
	for (var s in vsa) {
		if (vsa[s]["latitude"] && vsa[s]["longitude"]) {
			html = '<div style="font-family: Arial; line-height: 1.5em; font-size: 10pt;">'
			html += '<b>Deployment: </b>' + vsa[s]["deployment"] + '<br />';
			html += '<b>Station: </b>' + s + '<br />';
			html += '<b>Sensors:</b><br />';
			tmp = dvos[vsa[s]["deployment"]][s];
			for (t in tmp) {
				html += '<div style="font-size: 9pt; margin-bottom: 4px; border-bottom: 1px dashed #555;">' + tmp[t] + '</div>';	
			}

			map.addMarker(vsa[s]["id"], parseFloat(vsa[s]["latitude"]), parseFloat(vsa[s]["longitude"]), html);
		}
		else
			if (coordinatesMissingErrorShown == false) {
				popUp.show(null, null, "Some sensor could not be placed on the map " +
									   "because of missing loongitude and latitude coordinates", true);
				coordinatesMissingErrorShown = true;
			} 
	}
}

// A Class that creates the map service selector that is placed
// on top of the map
function MapServiceSelector(map_service) {
	var width = 250;
	var left_pos = "" + ((window.width/2) - (width/2));
	
	this.div = document.createElement('div');
	this.div.id = "mapSelector"
	this.div.style.left = left_pos + "px";
	this.div.style.width = width + "px";
	
	this.update = function() {
		var html = '';
		
		if (map == Google)
			html = '<b>Google Maps</b> &nbsp; &nbsp; &nbsp; <a style="cursor:pointer;cursor:hand;" onClick="setMapService(\'search_ch\');">Switzerland</a>';
		else if (map == SearchCh)
			html = '<a style="cursor:pointer;cursor:hand;" onClick="setMapService(\'google\')">Google Maps</a> &nbsp; &nbsp; &nbsp; <b>Switzerland</b>';
		else
			alert("Map service unknown");
		
		this.div.innerHTML = html;
	}

	overlays.push(this.div);
}

var POPUP_IMAGE_ERROR = "map_warning.gif";
var POPUP_IMAGE_LOADING = "map_loader.gif";
function PopUp() {
	var width =  300;
	var height = 200;
	var top_pos = "" + ((window.height/2) - (height/2));
	var left_pos = "" + ((window.width/2) - (width/2));

	this.div_bg = document.createElement('div');
	this.div_bg.id = "mapDim"
	this.div_bg.style.width = window.width + "px";
	this.div_bg.style.height = window.height + "px";
	overlays.push(this.div_bg);

	this.div = document.createElement('div');
	this.div.id = "mapPopUp"
	this.div.style.width = width + "px";
	this.div.style.left = left_pos + "px";
	this.div.style.top = top_pos + "px";
	overlays.push(this.div);
	
	this.show = function(image, header, text, allowClose) {
		
		if (allowClose == true)
			this.div.innerHTML = '<div style="position: absolute; left:320px; top: 3px;" width: 300px; text-align: right;">' + 
							 	 '<span style="cursor:pointer; cursor: hand;" onClick="window.popUp.off()"><b>X</b></span></div><br>';
		else
			this.div.innerHTML = "<br>";

		if (image != null)
			this.div.innerHTML += '<img src="/images/' + image + '" style="margin-bottom: 10px;"/><br>';

		if (header != null)
			this.div.innerHTML += '<b>' + header + '</b><br><br>';
		
		if (text != null)
			this.div.innerHTML += '<span style="color: #555">' + text + '</span>';
		
		if (allowClose == true)
			this.div.innerHTML += '<br><div style="padding-top: 10px; cursor:pointer; cursor: hand;" ' +
								  'onClick="window.popUp.off()"><u>Close</u></span></div><br>';
		
		this.on();
	}

	this.on = function() {
		this.div.style.visibility = "visible";
		this.div_bg.style.visibility = "visible";
	}
	
	this.off = function() {
		this.div.style.visibility = "hidden";
		this.div_bg.style.visibility = "hidden";
	}
	
	this.off();
}


var Map = {
	unload: function() {
		// return if map is not loaded
		if (map == null)
			return;
		
		// Save the state of the map such as the zoom level
		// the map type and the position
		map.saveState();

		// Remove all the DOM elements from the DOM tree
		container.removeChild(map.div);
		for (o in overlays)
			container.removeChild(overlays[o]);
	}
	
	,load: function() {
		if (map.div == null) {
			map.div = document.createElement('div');
			map.div.style.width = width + "px";
			map.div.style.height = height + "px";
		}
		
		container.appendChild(map.div);
		Map.addOverlays();
		
		map.load();
	}
	
	,addOverlays: function() {
		for (o in overlays)
			container.appendChild(overlays[o]);
	}
}


var Google = {
	// The map object
	map: null
	
	// The div containing the map
	,div: null
	
	// All the markers added to the map
	,markers: null

	,load: function() {
		if (GBrowserIsCompatible()) {
			// Create the map if it's not already done
			if (this.map == null) {
				this.div.id = "div_google"
				
				this.map = new GMap2(this.div);
				this.map.addControl(new GLargeMapControl());
				this.map.addControl(new GHierarchicalMapTypeControl());
				this.map.addMapType(G_PHYSICAL_MAP);
				this.map.addMapType(G_SATELLITE_3D_MAP);
			}

			switch(map_type) {
				case "street":
					t = G_NORMAL_MAP; break;
				case "satellite":
					t = G_SATELLITE_MAP; break;
				case "hybrid":
					t = G_HYBRID_MAP; break;
				case "earth":
					t = G_SATELLITE_3D_MAP; break;
				case "terrain":
					t = G_PHYSICAL_MAP; break;
			}

			this.map.setMapType(t)
			this.map.setCenter(new GLatLng(center_lat, center_lng), zoom);
		}
	}

	,addMarker: function(id, latitude, longitude, html) {
		if (this.markers == null)
			this.markers = new Array();
		
		var latlng = new GLatLng(latitude, longitude);
		var marker = new GMarker(latlng);

		this.markers[id] = marker;
		this.map.addOverlay(marker);

		GEvent.addListener(marker, 'click',
			function() {
				marker.openInfoWindowHtml(html);
			}
		);
	}
	
	,showMarker: function(id) {
		if (this.markers[id] != null)
			this.markers[id].show();
		else
			alert(id + " is an unknown marker id");
	}

	,hideMarkers: function() {
		for (var i in this.markers)
			this.markers[i].hide();
	}
	
	// Saves the state of the map before unloading the map
	,saveState: function() {
		// Save the zoom level
		zoom = this.map.getZoom();

		// Save latitude and longitude for the center of the map
		center_lat = this.map.getCenter().lat();
		center_lng = this.map.getCenter().lng();

		// Save the map type
		switch(this.map.getCurrentMapType()) {
			case G_NORMAL_MAP:
				map_type = "street"; break;
			case G_HYBRID_MAP:
				map_type = "hybrid"; break;
			case G_SATELLITE_MAP:
				map_type = "satellite"; break;
			case G_SATELLITE_3D_MAP:
				map_type = "earth"; break;
			case G_PHYSICAL_MAP:
				map_type = "terrain"; break;
		}
	}
};


var SearchCh = {
	// The map object
	map: null
	
	// The div containing the map
	,div: null
	
	// All the Points of Interests added to the Search.Ch map.
	,markers: null

	,load: function() {
		if (SearchChMap.isBrowserCompatible()) {
			// Create the map if it's not already done
			if(this.map == null) {
				this.div.id = "div_search_ch"
				this.div.style.width = width + "px";

				this.map = new SearchChMap({ circle: false, container: "div_search_ch", poigroups: "" });
			}


			switch(map_type) {
				case "street":
					t = "street"; break;
				default:
					t = "aerial";
			}
			this.map.set({type: t,
					 container: "div_search_ch",
					 center: [center_lat, center_lng],
					 zoom: Util.googleZoom2searchCh(zoom)
			});
			
			// This event is a workaround for a bug in search.ch maps.
			// When zoom level is changed all hidden markers become visible.
			// This force a redraw of the markers after each zoom change
			this.map.addEventListener("zoomend", function(e) {
				setSensorFilter(sensor_filter);
			});
		}
	}

	,showMarker: function(id) {
		if (this.markers[id] != null)
			this.markers[id].show();
		else
			alert(id + " is an unknown marker id");
	}
	
	,addMarker: function(id, latitude, longitude, html) {
		if (this.markers == null)
			this.markers = new Array();

		var poi = new SearchChPOI({ center:[latitude, longitude], html: html, title: " "});
		this.map.addPOI(poi);
		this.markers[id] = poi;
	}

	,hideMarkers: function() {
		for (var i in this.markers)
			this.markers[i].hide();
	}
	
	,saveState: function() {
		// Save the map type
		switch (this.map.get("type")) {
			case "street":
				map_type = "street"; break;
			case "aerial":
				map_type = "satellite"; break;
		}

		// Save latitude and longitude for the center of the map
		var coords = this.map.get('center').split(',');
        var cox = Math.round(parseInt(coords[1]) - (this.map.centery * this.map.get().zoom)); 
        var coy = Math.round(parseInt(coords[0]) + (this.map.centerx * this.map.get().zoom));
        var wgs84coords = Util.fromCH1903toWGS84(cox , coy);
		center_lat = wgs84coords[1];
		center_lng = wgs84coords[0];

		// Save the zoom level
		zoom = Util.searchChZoom2google(this.map.get("zoom"));	
	}
};


var Util =  {
	// An array for transforming from google zoom levels to search.ch levels and back.
	// The index of the array is google's search level and the content is the corresponding
	// Search.ch zoom level.
	zoomTransformation: new Array (512, 512, 512, 512, 512, 512, 512, 512, 512, 128, 128, 32, 32, 8, 8, 2, 2, 1, 0.5, 0.5, 0.5)

	,googleZoom2searchCh: function(zoom) {
		return this.zoomTransformation[zoom];
	}

	,searchChZoom2google: function(zoom) {
		z = 0;
	
		// find the last occurance of the zoom in the transformation array
		for (var i=0; i != -1;) {
			z = i;
			i = Util.indexOfElement(this.zoomTransformation, zoom, ++i);
		}
		return z;
	}

	,fromCH1903toWGS84: function(x,y) {
		var yp = (y - 600000) / 1000000;
		var xp = (x - 200000) / 1000000;

		var lambdap =    2.6779094                                       
		              +  4.728982   * yp                                
		              +  0.791484   * yp              * xp              
		              +  0.1306     * yp              * Math.pow(xp, 2)  
		              -  0.0436     * Math.pow(yp, 3)                   ;
              
		var phip    =    16.9023892                                     
		              +  3.238272                     * xp              
		              -  0.270978   * Math.pow(yp,2)                    
		              -  0.002528                     * Math.pow(xp,2)  
		              -  0.0447     * Math.pow(yp,2)  * xp              
		              -  0.0140                       * Math.pow(xp,3)  ;

		var lambda  = lambdap * 1000 / 360;
		var phi     = phip    * 1000 / 360;

		return new Array(lambda, phi);    
	}
	
	// My own indexOf function for arrays, needed since IE doesn't provide one
	,indexOfElement: function(array, element, start) {
	    for (var i = (start || 0); i < array.length; i++) {
	      if (array[i] == element)
	        return i;
	    }
		return -1;
	}
};
