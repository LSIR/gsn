<html>
	<header>
	<title>Global Sensor Networks (GSN) Web Interface</title>
	</header>
	<body >

<jsp:include page="header.html"/>
<%@ page import="gsn.*,gsn.beans.DataField,gsn.beans.VSensorConfig,java.util.Iterator" %>
<tr height="90%">
<td>
<center>
<h2> Exported Data Structures (details)</h2>
<% Iterator<VSensorConfig>  it = Mappings.getAllVSensorConfigs ( );
   while ( it.hasNext () ) {
      VSensorConfig sensorIdentity = it.next ();
%>
<br>
<table cellpadding=0 cellspacing=0 border=0 width="75%">
<tr>
   <td colspan="4" bgcolor="lightgray">
      <center><I>Structure of the <b><%=sensorIdentity.getVirtualSensorName ()%></b></I></center>
   </td>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td><i>Sensor name : </i></td>
   <td><%=sensorIdentity.getVirtualSensorName ()%></td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td><i>Rate is limited: </i></td>
   <td>
      <% if (sensorIdentity.getOutputStreamRate()<=0)
						 	   out.print("No");
						    else 
						 	   out.print("Yes"+(sensorIdentity.getOutputStreamRate()));
						 %>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td><i>Storage : </i></td>
   <td><b><%=sensorIdentity.getStorageHistorySize ()%></b> of
      <b><%=( sensorIdentity.isPermanentStorage () ? "Permanent Storage" : "Non-Permanent Storage" )%></b></td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td><i>Author : </i></td>
   <td><p><%=sensorIdentity.getAuthor ()%></p></td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td><i>Support email : </i></td>
   <td><p><%=sensorIdentity.getEmail ()%></p></td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td width="160" valign="top"><i>Description : </i></td>
   <td><p><%=sensorIdentity.getDescription ()%></p></td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr height=1>
   <td colspan="5" bgcolor="lightgray"/>
</tr>
<% for ( DataField field : sensorIdentity.getOutputStructure () ) { %>
<tr>
   <td colspan="5" bgcolor="lightgray">
      <center><I>Exported Fields:</I></center>
   </td>
</tr>

<tr>
   <td bgcolor="lightgray" width="1"/>
   <td>Field name :</td>
   <td><%=field.getFieldName ()%><td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td>Field type :</td>
   <td><%=field.getType ()%><td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr>
   <td bgcolor="lightgray" width="1"/>
   <td>Field description :</td>
   <td><%=field.getDescription ()%><td>
   <td bgcolor="lightgray" width="1"/>
</tr>
<tr height=1>
   <td colspan="4" bgcolor="lightgray">
   </td>
</tr>
<%}%>
<%-- <tr>
                   <td colspan="4" bgcolor="lightgray">
                      <center><I>Used Services:</I></center>
                   </td>
                </tr>

                <% for (RequestedService service :sensorIdentity.getRequestedServices() ) { %>
               <tr>
                  <td bgcolor="lightgray" width="1"/>
                  <td>Service Address :</td>
                  <td><%=service.getAddress()%><td>
                  <td bgcolor="lightgray" width="1"/>
                </tr>
               <tr>
                  <td bgcolor="lightgray" width="1"/>
                     <td>Access Query:</td>
                     <td><p><%=service.getQuery()%></p><td>
                  <td bgcolor="lightgray" width="1"/>
                </tr>
                <tr height=1>
                   <td colspan="4" bgcolor="lightgray">
                   </td>
                </tr>
                <% } %> --%>
</table>
<% } %>
<br>
</center>

</td>
</tr>
<jsp:include page="footer.html"/>