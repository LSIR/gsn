<style type="text/css">
    v\:* {
      behavior:url(#default#VML);
    }
</style>
<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAA2MV0HBY9S6-aGXtjN0b1ZxTNHpjHnpWHHgdGqnMBUpwQOS9hiRQYia2Wq5RjKhXafOPvhLZP4RAQOA" type="text/javascript"></script>
<script type="text/javascript">//<![CDATA[	
var map;

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

  		//attach event
  		GEvent.addListener(map, "click", function(overlay, point) {
			if(overlay) {	// when a marker is clicked
				//console.debug(overlay.id);
				GSN.menu(overlay.vsname);
			} else if(point) {	// when the background is clicked
				map.closeInfoWindow();
			}
		});
				
		$.ajax({ type: "GET", url: "/gsn", success: function(data){
			$("virtual-sensor",data).each(function(){
				var lat = $("field[@name=LATITUDE]",$(this)).text();
				var lon = $("field[@name=LONGITUDE]",$(this)).text();
				if (lat != "" && lon != ""){
					map.setCenter(new GLatLng(lat,lon), 13);
					var point = new GLatLng(lat,lon);
					var marker = new GMarker(point);
  					marker.vsname = $(this).attr("name");
					map.addOverlay(marker);
				}
  			});
		}});
   	}
});


//]]></script>
<h2>Global Sensor Network Map</h2>
<div id="map" style="width: 100%; height: 500px;"></div>
<div id="vs"></div>		