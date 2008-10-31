
// The map currently in used, either set to Google or to SearchCh
var map = null;

// The currently applied sensor filter
var sensor_filter = null;

// Map type to use, possible values: "street", "hybird", "satelite",
// "earth" or "terrain"
var map_type = "terrain";

// The div that surrounds the map divs
var container;

// An instance of the selector which builds the div containing
// the map service selector
var selector;

// Virtual sensor addressing information
var vsa = null;

// Map default parameters
var width = 848;
var height = 800;
var center_lat = 46.82261;
var center_lng = 8.22876;
var zoom = 8;

// Delay used before updating the markers, is used since search.ch
// is buggy and the map needs time to be drawn before you can modify markers
var delay = 250;

// Draws the map into to the div with id map_container and draws the sensors onto the map
// filtered with information in sensor_filter (if null, draw all sensors). The map service
// used is set by map_service (either "google" or "search_ch")
function init(map_service, map_container, sensor_filter) {
	// Make google the default map
	if (map_service == null)
		map_service = "google";
	
	// Get the div which contains the map
	var div = document.getElementById(map_container);
	div.style.width = width + "px";
	div.style.height = height + "px";
	div.style.zIndex = "1";
	
	// Create a new div which will contain the map and
	// the map service selector
	container = document.createElement('div');
	container.style.width = width + "px";
	container.style.height = height + "px";
	container.style.position ="absolute"
	div.appendChild(container);
	
	// Create the selector, it will be added on top of the map
	// when the map is loaded
	selector = new MapServiceSelector(map_service);

	sensor_filter = sensor_filter;

	setMapService(map_service);
	loadVsa();
}

// Switch the map service
function setMapService(map_service) {
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

	map.unload();
	map.load();
	
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
	$(function(){
	    $.getJSON("/map/vsa2",
	    function(data){
			vsa = data;
			addSensorsToMap();
			setSensorFilter(sensor_filter);
	    });
	  });
}

function addSensorsToMap() {
	for (var s in vsa) {
		if (vsa[s]["latitude"] && vsa[s]["longitude"]) {
			html = '<div class="stationInformation">'
			html += '<table><tr><td><b>Deployment:&nbsp;&nbsp;</b></td><td>' + vsa[s]["deployment"] + '</td></tr>';
			html += '<tr><td><b>Station:</b></td><td>' + s + '</td></tr>';
			html += '<tr><td><b>Sensors:</b></td><td></td></tr></table>';
			tmp = dvos[vsa[s]["deployment"]][s];
			for (t in tmp) {
				html += '<div id="sensor">' + tmp[t] + '</div>';	
			}

			map.addMarker(vsa[s]["id"], parseFloat(vsa[s]["latitude"]), parseFloat(vsa[s]["longitude"]), html);
			alert(html)
		}
	}
}

// A Class that creates the map service selector that is placed
// on top of the map
function MapServiceSelector(map_service) {
	var width = 230;

	var google_selected = "";
	var search_ch_selected = "";
	
	switch(map_service) {
		case "google":
			google_selected = ' selected="yes"'; break;
		case "search_ch":
			search_ch_selected = ' selected="yes"'; break;
		default:
			alert("Map service unknown");
	}

	this.div = document.createElement('div');
	this.div.id = "mapSelector"
	this.div.style.width = width + "px";
	this.div.style.left = "" + ((window.height/2) - (width/2)) + "px";
	this.div.innerHTML = ''
		+ '<b>Map service:</b> <select id="map_service" onchange="setMapService(this.value);">'
		+	'<option value="google"' + google_selected + '">Google Maps</option>'
		+	'<option value="search_ch"' + search_ch_selected + '>Switzerland</option>'
		+ '</select>';
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
				this.div = document.createElement('div');
				this.div.style.width = width + "px";
				this.div.style.height = height + "px";
				this.div.id = "div_google"

				container.appendChild(this.div);
				container.appendChild(selector.div);

				this.map = new GMap2(this.div);
				this.map.addControl(new GLargeMapControl());
				this.map.addControl(new GHierarchicalMapTypeControl());
				this.map.addMapType(G_PHYSICAL_MAP);
				this.map.addMapType(G_SATELLITE_3D_MAP);
			}
			// If map already exists, just add it tot the DOM tree
			else {
				container.appendChild(this.div);
				container.appendChild(selector.div);
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

			var map = this.map;
			var map_state_changed = function() {
				// Save the zoom level
				zoom = map.getZoom();

				// Save latitude and longitude for the center of the map
				center_lat = map.getCenter().lat();
				center_lng = map.getCenter().lng();

				// Save the map type
				switch(map.getCurrentMapType()) {
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

			// Add event listeners for updating the global variables when the user
			// change the parameters of the map
			GEvent.addListener(map, "zoomend", map_state_changed);
			GEvent.addListener(map, "moveend", map_state_changed);
			GEvent.addListener(map, "maptypechanged", map_state_changed);
		}
	}

	,unload: function() {
		a = container.childNodes[0];
		b = container.childNodes[1];
		
		// Remove the map div from the DOM tree
		if (a != null)
			container.removeChild(a);
		if (b != null)
			container.removeChild(b);
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
				this.div = document.createElement('div');
				this.div.id = "div_search_ch"
				this.div.style.width = width + "px";
				this.div.style.height = height + "px";
				container.appendChild(this.div);
				container.appendChild(selector.div);

				this.map = new SearchChMap({ circle: false, container: "div_search_ch", poigroups: "" });
			}
			// If map already exists, just add it tot the DOM tree
			else {
				container.appendChild(this.div);
				container.appendChild(selector.div);
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

			var map = this.map;
			var map_state_changed = function(e) {
				// Save the map type
				switch (map.get("type")) {
					case "street":
						map_type = "street"; break;
					case "aerial":
						map_type = "satellite"; break;
				}

				// Save latitude and longitude for the center of the map
				var coords = map.get('center').split(',');
		        var cox = Math.round(parseInt(coords[1]) - (map.centery * map.get().zoom)); 
		        var coy = Math.round(parseInt(coords[0]) + (map.centerx * map.get().zoom));
		        var wgs84coords = Util.fromCH1903toWGS84(cox , coy);
				center_lat = wgs84coords[1];
				center_lng = wgs84coords[0];

				// Save the zoom level
				zoom = Util.searchChZoom2google(map.get("zoom"));
			}

			// Add event listeners for updating the global variables when the user
			// change the parameters of the map
			this.map.addEventListener("maptypechanged", map_state_changed);
			this.map.addEventListener("dragend", map_state_changed);
			this.map.addEventListener("zoomend", map_state_changed);
		}
	}

	,unload: function() {
		// Exactly the same as for unloading a google map
		Google.unload();
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
			i = this.zoomTransformation.indexOf(zoom, ++i);
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
};

//}