<%@page contentType="text/xml; charset=UTF-8" import="gsn.*,java.util.*,gsn.beans.*,gsn.storage.*,gsn.web.*,java.sql.*" %><% 
  Integer refresh_rate = HttpRequestUtils.getIntParameter ( WebConstants.REFRESH_RATE_KEY , WebConstants.DEFAULT_REFRESH_RATE , request ) ;
  Integer pageSize = HttpRequestUtils.getIntParameter ( WebConstants.NUM_OF_ITEMS_KEY , WebConstants.DEFAULT_NUM_OF_ITEMS_SHOWN , request ) ;
  Integer start = HttpRequestUtils.getIntParameter ( WebConstants.OUTPUT_START_INDEX , Integer.MAX_VALUE , request ) ;
  String vsName = request.getParameter ( "vs" ) ;
  VSensorConfig virtualSensorInstance = Mappings.getVSensorConfig ( vsName ) ;
  if ( vsName == null) {
	  Iterator<VSensorConfig>  it = Mappings.getAllVSensorConfigs ( );
	  %><?xml version="1.0" encoding="utf-8" ?>
	  <rsp stat="ok">
	    <virtualsensors><% while ( it.hasNext () ) { vsName = it.next ().getVirtualSensorName (); %>
         <virtualsensor name="<%=vsName%>" />
        <% } %></virtualsensors>
	  </rsp><% 
	return;
  } else if  ( virtualSensorInstance == null ){ 
	%><?xml version="1.0" encoding="utf-8" ?>
<rsp stat="fail">
	<err code="404" msg="Virtualsensor not found!" />
</rsp><% 
	return;
  }
  StringBuilder queryStatement  = new StringBuilder ( "select * from " ).append ( vsName ) ;
    if ( pageSize > - 1 )
		 queryStatement.append ( " where PK <" ).append ( start ).append ( " order by TIMED DESC LIMIT " ).append ( pageSize ).append ( " offset 0" ) ;
   ResultSet result = StorageManager.getInstance ( ).executeQueryWithResultSet ( queryStatement.toString ( )) ;
   ArrayList<DataField> dataFields = virtualSensorInstance.getOutputStructure ( );
//   Integer    pageStartIndex = ( Integer ) request.getAttribute ( WebConstants.OUTPUT_START_INDEX );
	
    response.setHeader("Cache-Control","no-cache"); //HTTP 1.1
%><?xml version="1.0" encoding="utf-8" ?>
<rsp stat="ok">
   <time><%= new Time ( System.currentTimeMillis () ).toString () %></time>

<% long primaryKey = 0;
// list vs
while ( result.next () ) { 
	primaryKey = result.getLong ( "PK" );
%>
<virtualsensor name="<%=vsName%>" id="<%=primaryKey%>">
<%  for ( int i = 2; i <= result.getMetaData ().getColumnCount () ; i++ ) { 
	if ( result.getMetaData ().getColumnName ( i ).equalsIgnoreCase ( "PK" ) ) continue;
	
%><field name="<%=result.getMetaData ().getColumnName ( i ).toLowerCase()%>" <% 
	if ( result.getMetaData ().getColumnType ( i ) == Types.VARBINARY  || result.getMetaData ().getColumnType ( i ) == Types.BINARY || result.getMetaData ().getColumnType ( i ) == Types.BLOB || result.getMetaData ().getColumnType ( i ) == Types.LONGVARBINARY) {
    String mimeType = null;
    for (DataField fieldName : dataFields)
       if(fieldName.getFieldName ().equalsIgnoreCase ( result.getMetaData ().getColumnName ( i))){
         StringTokenizer stringTokenizer=new  StringTokenizer(fieldName.getType (),":");
         if (stringTokenizer.countTokens ()<2)
          break;
         else {
            stringTokenizer.nextToken ();
            mimeType = stringTokenizer.nextToken ().trim ();
         }
       }
if (mimeType.equalsIgnoreCase ( "jpeg")||mimeType.equalsIgnoreCase ( "png")||mimeType.equalsIgnoreCase ( "jpg")|| mimeType.equalsIgnoreCase ( "gif")) {
out.print("type=\"image\">"+java.net.URLEncoder.encode("/field?vs="+vsName+"&identity="+primaryKey+"&field="+result.getMetaData().getColumnName (i),"UTF-8"));
} else if (mimeType.equalsIgnoreCase ( "svg")) {
out.print("type=\"svg\">"+java.net.URLEncoder.encode("/field?vs="+vsName+"&identity="+primaryKey+"field="+result.getMetaData().getColumnName (i),"UTF-8"));
} else {
out.print("type=\"\">"+"Not Specificed");
}
	} else if ( result.getMetaData ().getColumnName ( i ).toLowerCase().startsWith("time") ) {
        	out.print("type=\"time\">"+new Time ( Long.parseLong ( result.getString ( i ) ) )+"."+(Long.parseLong ( result.getString ( i ) )%1000));
        } else { 
        	out.print("type=\"double\">"+result.getString ( i ));
        } %></field>
<%}%>
</virtualsensor>
<%} //end loop on vs%>

<%  StorageManager.getInstance ().returnResultSet ( result );  %>
</rsp>