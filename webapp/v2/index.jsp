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
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html lang="en">
<head>
	<title><%=name%> :: GSN</title>
	<link rel="stylesheet" href="style/gsn.css" type="text/css" media="screen,projection" />
	<script type="text/javascript" src="js/jquery-latest.js"></script>
	<script type="text/javascript" src="js/jquery-dom.js"></script>
	<script type="text/javascript" src="js/gsn.js"></script>
</head>
<body>

<div id="container">
	<div id="header">
		<a href="."><h1><%=name%> :: GSN</h1></a>
	</div>
	<div id="nav">
		<ul>
			<li><a href=".">home</a></li>
			<li><a href="?p=reg">registeration</a></li>
			<li><a href="?p=data">data</a></li>
			<li><a href="?p=map">map</a></li>
			<!-- <li><a href="fullmap.jsp">fullmap</a></li> -->
		</ul>
	</div>
	<div id="main">
		<noscript><p class="error">Your browser doesn't appear to support JavaScript. This is most likely because you're using a text-based or otherwise non-graphical browser. Sadly, GSN require javascript in order to work properly. If you want to access directly the data, you can use the api at <a href="http://localhost:22001/gsn">http://localhost:22001/gsn</a>.</p></noscript>
		<jsp:include flush="true" page='<%=requestedpage%>' />
	</div>
	<div id="sidebar">
		<h3>Desctiption : </h3>
		<p><%=description%></p>
		<h3>Author : </h3>
		<p><%=author%> (<%=email%>)</p>
		<h3>Links : </h3>
		<ul>
			<li><a href="registeration.jsp">How to address the sensorIdentity(s)</a></li>
			<li><a href="structure.jsp">Data structure of indivitual sensorIdentity</a></li>
			<li><a href="search.jsp">Searching recent observations</a></li>
		</ul>
		<h3> Virtual sensors : </h3>
        <ul><% 
        Iterator<VSensorConfig>  it = Mappings.getAllVSensorConfigs ( );
        while ( it.hasNext () ) { String vsName = it.next().getVirtualSensorName (); %>
         <li><a href="javascript:GSN.menu('<%=vsName%>');"><%=vsName%></a></li>
        <% } %></ul>
	</div>
	<div id="footer">
		<p>Powered by <a href="http://globalsn.sourceforge.net/">GSN</a>,  Distributed Information Systems Lab, EPFL 2006</p	>
	</div>
</div>
</body>
</html>