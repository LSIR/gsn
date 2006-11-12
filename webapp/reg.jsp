<%@ page import="gsn.*,gsn.beans.VSensorConfig,org.apache.commons.collections.KeyValue,java.util.Iterator" %>
<% Iterator<VSensorConfig>  it = Mappings.getAllVSensorConfigs ( ); %>

<h2>Addressing/Registeration information</h2>
<% while ( it.hasNext() ) {
	VSensorConfig sensorIdentity = it.next (); %><div class="vsbox">
	<h3><%=sensorIdentity.getVirtualSensorName ()%></h3>
    <dl>
    <% for ( KeyValue predicate : sensorIdentity.getAddressing () ) { %>
    <dt><%=predicate.getKey()%></dt>
	<dd><%=predicate.getValue()%></dd>
	<% } %>
	</dl>
</div><% } %>