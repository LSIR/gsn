<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAA2MV0HBY9S6-aGXtjN0b1ZxTNHpjHnpWHHgdGqnMBUpwQOS9hiRQYia2Wq5RjKhXafOPvhLZP4RAQOA" type="text/javascript"></script>
<script type="text/javascript">
<!--//<![CDATA[
$(document).ready(function() {
	//bind buttons to javascript functionality
	$("#refreshall_timeout").bind("change",GSN.updateall);
	
	//init map
	GSN.map.init();
	GSN.updateall();
});
//]]>-->
</script>
<h2>Global Sensor Network Map</h2>
<p>refresh every msec : 
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
</p>
<div id="map" style="width: 100%; height: 500px;"></div>
<div id="vs"></div>		