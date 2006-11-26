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
<!-- <h2>Global Sensor Network</h2> -->
<div class="intro">Welcome to Global Sensor Networks. All sensors are displayed by default, but you can easily close them with the close all button. By clicking on a virtual sensors on the left sidebar, it will bring it to the top of the list.</div>
<p>
Auto-refresh every : 
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
	<div class="loading">Virtual sensors are currently loading...</div>
</div>		