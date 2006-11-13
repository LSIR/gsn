<style type="text/css">
    v\:* {
      behavior:url(#default#VML);
    }
</style>
<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAA2MV0HBY9S6-aGXtjN0b1ZxTNHpjHnpWHHgdGqnMBUpwQOS9hiRQYia2Wq5RjKhXafOPvhLZP4RAQOA" type="text/javascript"></script>
<script type="text/javascript">//<![CDATA[	
var map;
var tinyred;
var tinygreen;

$(document).ready(function() {
	if(typeof GBrowserIsCompatible == "undefined") {
		$("#map").append($.P({"class":"error"},"Google maps isn't loaded! Maybe your internet connection is not working."));
	} else if (GBrowserIsCompatible()) {
		GSN.debug("init gmap");
       
        map = new GMap2(document.getElementById("map"));
        //some fun
        map.addControl(new GLargeMapControl());
		map.addControl(new GMapTypeControl());
		map.addControl(new GScaleControl());
		map.addControl(new GOverviewMapControl());


		// Create our "tiny" marker icon
		tinyred = new GIcon();
		tinyred.image = "http://labs.google.com/ridefinder/images/mm_20_red.png";
		tinyred.shadow = "http://labs.google.com/ridefinder/images/mm_20_shadow.png";
		tinyred.iconSize = new GSize(12, 20);
		tinyred.shadowSize = new GSize(22, 20);
		tinyred.iconAnchor = new GPoint(6, 20);
		tinyred.infoWindowAnchor = new GPoint(5, 1);
		
		// Create our "tiny" marker icon
		tinygreen = new GIcon();
		tinygreen.image = "http://labs.google.com/ridefinder/images/mm_20_green.png";
		tinygreen.shadow = "http://labs.google.com/ridefinder/images/mm_20_shadow.png";
		tinygreen.iconSize = new GSize(12, 20);
		tinygreen.shadowSize = new GSize(22, 20);
		tinygreen.iconAnchor = new GPoint(6, 40);
		tinygreen.infoWindowAnchor = new GPoint(5, 1);

		
  		//attach event
  		GEvent.addListener(map, "click", function(overlay, point) {
			if(overlay)	//when a marker is clicked
				if(typeof overlay.vsname != "undefined") 
					GSN.menu(overlay.vsname);
		});		
		GEvent.addListener(map, 'zoomend', function (oldzoomlevel,newzoomlevel) {
  			GSN.map.zoomend(oldzoomlevel,newzoomlevel);
		}); 
		
			
		
		/*
		$.ajax({ type: "GET", url: "/gsn", success: function(data){
			$("virtual-sensor",data).each(function(){
				var lat = $("field[@name=latitude]",$(this)).text();
				var lon = $("field[@name=longitude]",$(this)).text();
				if (lat != "" && lon != ""){
					GSN.map.addMarker($(this).attr("name"),lat,lon);
				}
  			});
  			GSN.map.showAllMarkers();
		}});*/
		
		$("#refreshall_timeout").bind("change",GSN.updateall);
		GSN.updateall(true);
   	}
});


//]]></script>
<h2>Global Sensor Network Map</h2>
<form><p>refresh every msec : 
<select id="refreshall_timeout" >
<option value="3600000">1hour</option> 
<option value="600000">10min</option> 
<option value="60000" selected="selected">1min</option> 
<option value="30000">30sec</option> 
<option value="5000">5sec</option> 
<option value="1000">1sec</option> 
<option value="0">disable</option> 
</select>
<input id="refreshall_autozoomandcenter" type="checkbox" checked="checked" />auto zoom and center
</p></form>
<div id="map" style="width: 100%; height: 500px;"></div>
<div id="vs"></div>		