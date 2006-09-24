<html>
	<header>
	<title>Global Sensor Networks (GSN) Web Interface</title>
	</header>
	<body >
<jsp:include page="header.html"/>
<%@ page import="gsn.beans.VSensorConfig,org.apache.commons.collections.KeyValue,java.util.Iterator" %>

<tr height="90%">
   <td>
      <center>
         <h2> Addressing/Registeration information</h2>
         <%
            Iterator<VSensorConfig>  it = ( Iterator ) request.getAttribute ( "sensors" );
            while ( it.hasNext () ) {
               VSensorConfig sensorIdentity = it.next ();
         %>
         <table width="75%" cellpadding=0 cellspacing=0 border=0>
            <tr>
               <td colspan="4" bgcolor="lightgray">
                  <center><I>Addressing of the <b><%=sensorIdentity.getVirtualSensorName ()%></b></I></center>
               </td>
            </tr>
            <tr>
               <td bgcolor="lightgray" width="1"/>
               <td>Virtual Sensor Name : </td>
               <td><%=sensorIdentity.getVirtualSensorName ()%></td>
               <td bgcolor="lightgray" width="1"/>
            </tr>
            <tr height=1>
               <td colspan="4" bgcolor="lightgray"/>
            </tr>
            <% for ( KeyValue predicate : sensorIdentity.getAddressing () ) { %>
            <tr>
               <td bgcolor="lightgray" width="1"/>
               <td>Predicate Name :</td>
               <td><%=predicate.getKey ()%><td>
               <td bgcolor="lightgray" width="1"/>
            </tr>
            <tr>
               <td bgcolor="lightgray" width="1"/>
               <td>Predicate Value :</td>
               <td><%=predicate.getValue ()%><td>
               <td bgcolor="lightgray" width="1"/>
            </tr>
            <tr height=1>
               <td colspan="4" bgcolor="lightgray"/>
            </tr>
            <% } %>
         </table>
         <% } %>
         <br>
      </center>

   </td>
</tr>
<jsp:include page="footer.html"/>