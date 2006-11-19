<%@ page import="gsn.*,gsn.beans.VSensorConfig,java.util.Iterator,gsn.web.*" %>
<%
	String requestedpage = request.getParameter("p") +".jsp";
    if (!(requestedpage.equals("map.jsp")
       || requestedpage.equals("reg.jsp")
       || requestedpage.equals("data.jsp"))) 
    	requestedpage = "main.jsp";

 	String name = Main.getContainerConfig ( ).getWebName ( ) ;
    String author = Main.getContainerConfig ( ).getWebAuthor ( )  ;
    String email = Main.getContainerConfig ( ).getWebEmail ( ) ;
    String description = Main.getContainerConfig ( ).getWebDescription ( ) ;
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html lang="en">
<head>
	<title><%=name%> :: GSN</title>
	<link rel="stylesheet" href="style/gsn.css" type="text/css" media="screen,projection" />
	<script type="text/javascript" src="js/jquery-latest.js"></script>
	<script type="text/javascript" src="js/jquery-dom.js"></script>
	<script type="text/javascript" src="js/gsn.js"></script>
	<script type="text/javascript" src="js/dimensions.js"></script>
	<script src="http://maps.google.com/maps?file=api&amp;v=2&amp;key=ABQIAAAA2MV0HBY9S6-aGXtjN0b1ZxTNHpjHnpWHHgdGqnMBUpwQOS9hiRQYia2Wq5RjKhXafOPvhLZP4RAQOA" type="text/javascript"></script>
<script type="text/javascript">
<!--//<![CDATA[
$(document).ready(function() {
	GSN.map.init();
	GSN.updateall();

	$("#vs").height($(window).height() - $("#vs").offset().top - 10)
	
	$(window).resize( function() { $("#vs").height($(window).height() - $("#vs").offset().top - 10) } );
});	
//]]>-->
</script>
</head>
<body style="margin:0;padding:0;width:100%;height:100%;background:#fff;overflow:hidden;">
<div id="map" style="display: block; position: relative; height: 100%; margin: 0 330px 0 0;"></div>
<div style="position: absolute; right: 0; top: 0; width: 330px; height:100%;">
	<div id="headerfm"><h1><a href="."><%=name%> :: GSN</h1></a></div>
	
<div id="navigation">
		<ul>
			<li><a href="/">home</a></li>
			<li><a href="/?p=data">data</a></li>
			<li><a href="/?p=map">map</a></li>
			<li class="selected"><a href="fullmap.jsp">fullmap</a></li>
		</ul>
	</div>
	
	<div id="mainx" style="padding:0 2px;">
		<noscript><p class="error">Your browser doesn't appear to support JavaScript. This is most likely because you're using a text-based or otherwise non-graphical browser. Sadly, GSN require javascript in order to work properly. If you want to access directly the data, you can use the api at <a href="http://localhost:22001/gsn">http://localhost:22001/gsn</a>.</p></noscript>

<h2>Global Sensor Network</h2>
<!-- <form><p>refresh every msec : 
<select id="refreshall_timeout" >
<option value="3600000">1hour</option> 
<option value="600000">10min</option> 
<option value="60000" selected="selected">1min</option> 
<option value="30000">30sec</option> 
<option value="5000">5sec</option> 
<option value="1000">1sec</option> 
<option value="0">disable</option> 
</select>
<input id="refreshall" type="button" value="refresh" /><br />
<input id="refreshall_autozoomandcenter" type="checkbox" checked="checked" />auto zoom and center
<input id="closeall" type="button" value="close all" />
</p></form> -->
<div id="vs" style="overflow: auto;width:100%;height:50%	">
<div class="loading">Virtual sensors are currently loading...</div>
</div>		
</div>
</body>
</html>