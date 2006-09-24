<html>
	<header>
	<title>Global Sensor Networks (GSN) Web Interface</title>
	</header>
	<body >
<jsp:include page="header.html"/>
<%@ page import="gsn.beans.VSensorConfig,java.util.Iterator,gsn.web.*" %>
<tr height="90%">
   <td>
      <center>
         <h1>Welcome to <%=request.getAttribute ( "name" )%></h1>

         <h3>Desctiption : </h3>

         <p><%=request.getAttribute ( "description" )%></p>

         <h3>Author : <%=request.getAttribute ( "author" )%> (<%=request.getAttribute ( "email" )%>)</h3>

         <h3> The container hosts the following virtual sensors : </h3>
         <%
            Iterator<VSensorConfig>  it = ( Iterator ) request.getAttribute ( "sensors" );
            while ( it.hasNext () ) {
               String vsName = it.next ().getVirtualSensorName ();
         %>
         <h4><i><a href="/live.do?vs=<%=vsName%>&<%=AllDataFromVirtualSensor.NUM_OF_ITEMS_KEY%>=<%=AllDataFromVirtualSensor.DEFAULT_NUM_OF_ITEMS_SHOWN%>&<%=AllDataFromVirtualSensor.REFRESH_RATE_KEY%>=<%=AllDataFromVirtualSensor.DEFAULT_REFRESH_RATE%>"><%=vsName%></a></i></h4>
         <% } %>
         <table border="0">
            <tr align="left">
               <td>
                  <h4>
                     Information about how to address the sensorIdentity(s) is in
                     <a href="registeration.do">here</a>.<br>
                     Information about the data structure of indivitual sensorIdentity is in
                     <a href="structure.do">here</a>.<br>
                     For searching recent observations by hosted sensors click
                     <a href="search.jsp">here</a>.</h4>
               </td>
            </tr>
         </table>
      </center>

   </td>
</tr>
<jsp:include page="footer.html"/>