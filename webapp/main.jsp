<script type="text/javascript">
<!--//<![CDATA[
$(document).ready(function() {
	GSN.debug("main.jsp init");
	$("#refreshall_timeout").bind("change",GSN.updateall);
	GSN.updateall(true);
});	
//]]>-->
</script>
<h2>Global Sensor Network</h2>
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
<div id="vs"><div class="loading">Virtual sensors are currently loading...</div></div>		