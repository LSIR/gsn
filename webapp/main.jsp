<script type="text/javascript">
<!--//<![CDATA[
$(document).ready(function() {
	//bind buttons to javascript functionality
	$("#refreshall_timeout").bind("change",GSN.updateallchange);
	$("#refreshall").bind("click",GSN.updateall);
	$("#closeall").bind("click",GSN.closeall);
	
	//load and display all the visual sensors
	GSN.updateall();
});	
//]]>-->
</script>
<h2>Global Sensor Network</h2>
<p>
refresh every msec : 
<select id="refreshall_timeout" >
	<option value="3600000">1hour</option> 
	<option value="600000">10min</option> 
	<option value="60000" selected="selected">1min</option> 
	<option value="30000">30sec</option> 
	<option value="5000">5sec</option> 
	<option value="1000">1sec</option> 
	<option value="0">disable</option> 
</select>
<input id="refreshall" type="button" value="refresh" />
<input id="closeall" type="button" value="close all" />
<span class="refreshing"><img src="style/ajax-loader.gif" alt="loading" title="" /></span>
</p>
<div id="vs">
	<div class="intro">Welcome explaination... Click right...</div>
	<div class="loading">Virtual sensors are currently loading...</div>
</div>		