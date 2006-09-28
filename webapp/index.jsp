<%@ page import="gsn.*,gsn.beans.VSensorConfig,java.util.Iterator,gsn.web.*" %>
<html>
	<header>
	<title>Global Sensor Networks (GSN) Web Interface</title>
	</header>
	<body >
<jsp:include page="header.html"/>
<%
 	String name = Main.getContainerConfig ( ).getWebName ( ) ;
    String author = Main.getContainerConfig ( ).getWebAuthor ( )  ;
    String email = Main.getContainerConfig ( ).getWebEmail ( ) ;
    String description = Main.getContainerConfig ( ).getWebDescription ( ) ;
    Iterator<VSensorConfig>  it = Mappings.getAllVSensorConfigs ( );
%>


<tr height="90%">
   <td>
      <center>
         <h1>Welcome to <%=name%></h1>

         <h3>Desctiption : </h3>

         <p><%=description%></p>

         <h3>Author : <%=author%> (<%=email%>)</h3>

         <h3> The container hosts the following virtual sensors : </h3>
         <% while ( it.hasNext () ) { String vsName = it.next ().getVirtualSensorName (); %>
         <h4><i><a href="/live.jsp?vs=<%=vsName%>&<%=WebConstants.NUM_OF_ITEMS_KEY%>=<%=WebConstants.DEFAULT_NUM_OF_ITEMS_SHOWN%>&<%=WebConstants.REFRESH_RATE_KEY%>=<%=WebConstants.DEFAULT_REFRESH_RATE%>"><%=vsName%></a></i></h4>
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