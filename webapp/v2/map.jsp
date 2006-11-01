<style type="text/css">
    v\:* {
      behavior:url(#default#VML);
    }
</style>
<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAA2MV0HBY9S6-aGXtjN0b1ZxTNHpjHnpWHHgdGqnMBUpwQOS9hiRQYia2Wq5RjKhXafOPvhLZP4RAQOA" type="text/javascript"></script>
<script type="text/javascript">//<![CDATA[	
var map;

$(document).ready(function() {
	if (GBrowserIsCompatible()) {
		console.debug("init gmap");
        map = new GMap2(document.getElementById("map"));
        //some fun
        map.addControl(new GLargeMapControl());
		map.addControl(new GMapTypeControl());
		map.addControl(new GScaleControl());
		map.addControl(new GOverviewMapControl());
		
		
						
		$.ajax({ type: "GET", url: "api.jsp?vs=GPSVS", success: function(data){
		if ($("rsp",data).attr("stat")!="ok") {
			console.error("Error: " , $("err",data).attr("msg")); 
		} else {
			var lat = $("field[@name=latitude]",$("virtualsensor",data)).text();
			var lon = $("field[@name=longitude]",$("virtualsensor",data)).text();
			map.setCenter(new GLatLng(lat,lon), 13);
			var point = new GLatLng(lat,lon);
  			map.addOverlay(new GMarker(point));
		}
		}});
						
        
   	}
});


//]]></script>
<div id="map" style="width: 100%; height: 500px;"></div>
<div id="vs"></div>		