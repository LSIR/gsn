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
				//console.debug(overlay.getPoint());
			} else if(point) {	// when the background is clicked
				map.closeInfoWindow();
			}
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
</p></form>
<div id="map" style="width: 100%; height: 500px;"></div>
<div id="vs"></div>		