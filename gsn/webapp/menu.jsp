<%@ page import="gsn.Main,gsn.http.ac.*,org.apache.log4j.Logger" %>
<ul id="menu">
    <% String selected = request.getParameter("selected"); %>
    <li <%= "index".equals(selected) ? "class=\"selected\"" : "" %>><a href="index.html#home">home</a></li>
    <li <%= "data".equals(selected) ? "class=\"selected\"" : "" %>><a href="data.html#data">data browser</a></li>
    <li <%= "topology".equals(selected) ? "class=\"selected\"" : "" %>><a href="topology.html#topology">network topology</a></li>
    <li <%= "systemhealth".equals(selected) ? "class=\"selected\"" : "" %>><a href="systemhealth.html#systemhealth">system</a></li>
    <li <%= "nodehealth".equals(selected) ? "class=\"selected\"" : "" %>><a href="nodehealth.html#nodehealth">node health</a></li>
    <li <%= "basehealth".equals(selected) ? "class=\"selected\"" : "" %>><a href="basehealth.html#basehealth">base health</a></li>
    <li <%= "weather".equals(selected) ? "class=\"selected\"" : "" %>><a href="weather.html#weather">on-site weather</a></li>
    <li <%= "map".equals(selected) ? "class=\"selected\"" : "" %>><a href="map.html#map">map</a></li>
<!--     <li <%= "fullmap".equals(selected) ? "class=\"selected\"" : "" %>><a href="fullmap.html#fullmap">fullmap</a></li> -->
    <% if (Main.getContainerConfig().isAcEnabled()) { %>
        <li><a href="/gsn/MyAccessRightsManagementServlet">access rights management</a></li>
    <% } %>
</ul>
<% if (Main.getContainerConfig().isAcEnabled()) { %>
    <ul id="logintext"><%= displayLogin(request) %></ul>
<% } else { %>
    <ul id="linkWebsite"><li><a href="http://gsn.sourceforge.net/">GSN Home</a></li></ul>
<% } %>
<%!
private String displayLogin(HttpServletRequest req) {

  String name=null;
  HttpSession session = req.getSession();
  User user = (User) session.getAttribute("user");
  if (user == null)
    name="<li><a href=/gsn/MyLoginHandlerServlet> login</a></li>";
  else
  {
    name="<li><a href=/gsn/MyLogoutHandlerServlet> logout </a></li>"+"<li><div id=logintextprime >logged in as: "+user.getUserName()+"&nbsp"+"</div></li>";
  }
  return name;
}
%>