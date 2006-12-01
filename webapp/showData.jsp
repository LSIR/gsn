<%@ page import="gsn.*,gsn.beans.VSensorConfig,java.util.Iterator,gsn.web.*" %>
<%
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
<body onload="javascript:GSN.data.displayDatas('<%=request.getQueryString()%>')">

<div id="container">
	<div id="header">
		<h1><a href="."><%=name%> :: GSN</a></h1>
	</div>
	<div id="main">
		<noscript><p class="error">Your browser doesn't appear to support JavaScript. This is most likely because you're using a text-based or otherwise non-graphical browser. Sadly, GSN require javascript in order to work properly. If you want to access directly the data, you can use the api at <a href="http://localhost:22001/gsn">http://localhost:22001/gsn</a>.</p></noscript>
		<div id="datachooser">
		</div>
	</div>
	<div id="sidebar">
		<h3>Description</h3>
		<p><%=description%></p>
		<h3>Author</h3>
		<p><%=author%> (<%=email%>)</p>
	</div>
	<div id="footer">
		<p>Powered by <a href="http://globalsn.sourceforge.net/">GSN</a>,  Distributed Information Systems Lab, EPFL 2006</p	>
	</div>
</div>
</body>
</html>