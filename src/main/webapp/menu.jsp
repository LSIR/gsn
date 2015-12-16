<%@page language="java" contentType="text/html" pageEncoding="UTF-8" %>
<%@page import="gsn.http.ac.User" %>
<%@page import="com.typesafe.config.Config" %>
<%@page import="com.typesafe.config.ConfigFactory" %>
<!DOCTYPE html>
<html>
<head>
   <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
   <title>First JSP</title>
</head>
   
<body>

<%
 	Config conf=ConfigFactory.load();
 	boolean newDataPanel=conf.getBoolean("gsn.ui.menu.data.newVersion");
  	boolean fedLogin=conf.getBoolean("gsn.ac.federated");
	
    String selected = request.getParameter("selected");
%>
	<ul id="menu">
      	<li <%="index".equals(selected) ? " class=\"selected\"" : "" %>>
        	<a href="index.html#home">home</a></li>
      
      	<li <%="data".equals(selected) ? " class=\"selected\"" : "" %>>
        	<a href="data.html#data">data</a></li>
      
      	<li <%="map".equals(selected) ? " class=\"selected\"" : "" %>>
        	<a href="map.html#map">map</a></li>
      	<li <%="fullmap".equals(selected) ? " class=\"selected\"" : "" %>>
        	<a href="fullmap.html#fullmap">fullmap</a></li>
        
<% 
	if (gsn.Main.getContainerConfig().isAcEnabled()) { 
%>
       <li><a href="/gsn/MyAccessRightsManagementServlet">access rights</a></li>
<%  } %>
   
	</ul>
   
<% 	if (gsn.Main.getContainerConfig().isAcEnabled()) {  %>
       
   <ul id="logintext"> 

<%
	  User user = (User) session.getAttribute("user");
      if (user!=null) {
%>
		<li><a href="/gsn/MyLogoutHandlerServlet"> logout </a></li>
      	<li><div id=logintextprime >logged in as: <%=user.getUserName()%> &nbsp;</div></li>
<%    }  
      else if(fedLogin) { 
%>
       <li><a href="/gsn/MyLoginHandlerServlet?federated=true"> login</a></li>
       <li><a href="/gsn/MyUserCandidateRegistrationServlet">register</a></li>
<%    } else { %>
       <li><a href="/gsn/MyLoginHandlerServlet"> login</a></li>
       <li><a href="/gsn/MyUserCandidateRegistrationServlet">register</a></li>
       
    </ul>
<%    } 
    } 
    else { 
%>
    <ul id="linkWebsite"><li><a href="https://github.com/LSIR/gsn/">GSN Home</a></li></ul>
<%  } %>
       
</body>
</html>