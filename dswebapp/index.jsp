<%@page import="gsn.utils.*,gsn.registry.*,java.util.*,org.apache.commons.collections.KeyValue, gsn.shared.VirtualSensorIdentityBean" %>
<jsp:include page="/commons/header.html"/>
<tr height="90%">
			<td>
				<center>
					<h2> Welcome to GSN Directory Service : </h2>
					<h4> In the following pages you can find information about all<br>
						registered sensorIdentity networks with GSN infrastructure </h4>
					<% ArrayList<VirtualSensorIdentityBean> sensors = RegistryImp.getRegistry();
					   for ( VirtualSensorIdentityBean sensorIdentity : sensors ) { %>
					<table width="75%" cellpadding=0 cellspacing=0 border=0>
						<tr>
						 <td colspan="4" bgcolor="lightgray">
							 <center><I>Addressing of the <b><%=sensorIdentity.getVSName()%></b></I></center>
						 </td>
					 </tr>
					 <tr>
						<td bgcolor="lightgray" width="1"/>
						 <td>Sensor name : </td>
						 <td><%=sensorIdentity.getVSName()%></td>
						<td bgcolor="lightgray" width="1"/>
					 </tr>
					 <tr height=1>
						 <td colspan="4" bgcolor="lightgray"/>
					 </tr>
 				   <% for (KeyValue predicate : sensorIdentity.getPredicates() ) { %>
					<tr>
						<td bgcolor="lightgray" width="1"/>
						<td>Predicate Key :</td>
						<td><%=predicate.getKey()%><td>
						<td bgcolor="lightgray" width="1"/>
					 </tr>
					<tr>
						<td bgcolor="lightgray" width="1"/>
							<td>Predicate Value :</td>
							<td><%=predicate.getValue()%><td>
						<td bgcolor="lightgray" width="1"/>
					 </tr>
					 <tr bgcolor="silver">
						<td bgcolor="lightgray" width="1"/>
							<td colspan="2"><center>
								For more information click <A href="http://<%=sensorIdentity.getRemoteAddress()%>:<%=sensorIdentity.getRemotePort()%>/index.jsp">here</a>.
						</center></td>
						<td bgcolor="lightgray" width="1"/>
					 </tr>
					 <tr height=1>
						 <td colspan="4" bgcolor="lightgray"/>
					 </tr>
					 <br>
					<% } %>
				 </table>
					<% } %>
				 <br> 
<jsp:include page="/commons/footer.html" />
