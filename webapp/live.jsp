<%@page import="gsn.*,java.util.*,gsn.beans.*,gsn.storage.*,gsn.web.*,java.sql.*" %>
<% 
  Integer refresh_rate = HttpRequestUtils.getIntParameter ( WebConstants.REFRESH_RATE_KEY , WebConstants.DEFAULT_REFRESH_RATE , request ) ;
  Integer pageSize = HttpRequestUtils.getIntParameter ( WebConstants.NUM_OF_ITEMS_KEY , WebConstants.DEFAULT_NUM_OF_ITEMS_SHOWN , request ) ;
  Integer start = HttpRequestUtils.getIntParameter ( WebConstants.OUTPUT_START_INDEX , Integer.MAX_VALUE , request ) ;
  String vsName = request.getParameter ( "vs" ) ;
  VSensorConfig virtualSensorInstance = Mappings.getVSensorConfig ( vsName ) ;
  if ( vsName == null || virtualSensorInstance == null ){ %> <jsp:forward page="/index.jsp" /> <%
 	return;
  }
  StringBuilder queryStatement  = new StringBuilder ( "select * from " ).append ( vsName ) ;
    if ( pageSize > - 1 )
		 queryStatement.append ( " where PK <" ).append ( start ).append ( " order by TIMED DESC LIMIT " ).append ( pageSize ).append ( " offset 0" ) ;
   ResultSet result = StorageManager.getInstance ( ).executeQueryWithResultSet ( queryStatement.toString ( )) ;
   ArrayList<DataField> dataFields = virtualSensorInstance.getOutputStructure ( );
//   Integer    pageStartIndex = ( Integer ) request.getAttribute ( WebConstants.OUTPUT_START_INDEX );
%>
<html>
	<header>
		<title>Global Sensor Networks (GSN) Web Interface</title>
		<meta http-equiv="refresh" content="<%=refresh_rate%>"> 
    </header>
	<body >
<jsp:include page="header.html"/>
<tr height="90%">
   <td>
      <center>
         <h3> Live output from <i><%=vsName%></i> Virtual Sensor. </h3><br>
         <form name="/live.do" method="get">
			<input type="hidden" name="vs" value="<%=vsName%>" />
		 <table width="75%" cellpadding=0 cellspacing=0 border=0>
			  <tr>
        	       <td> Refresh rate (seconds) : </td>
               <td> <input type="text" name="<%=WebConstants.REFRESH_RATE_KEY%>" value=<%=refresh_rate%> /> </td>
	 	      </tr>
        	       <td> Number of items : </td>
               <td> <input type="text" name="<%=WebConstants.NUM_OF_ITEMS_KEY%>"  value=<%=pageSize%> /> </td>
 			  </tr><tr>
			  <td colspan="3" height="10"/>
 			  </tr><tr>
               <td colspan="3" align="center"> <input type="submit" value="update"/> </td>
            	  </tr>
		</table>
        </form>
         <br>
         <table width="75%" cellpadding=0 cellspacing=0 border=0>
            <tr>
               <td colspan="<%=(result.getMetaData().getColumnCount())*2+1%>"
                   bgcolor="lightgray">
                  <center><I>Results as of <%= new Time ( System.currentTimeMillis () ).toString () %></I></center>
               </td>
            </tr>
            <tr bgcolor="silver">
               <% for ( int i = 1; i <= result.getMetaData ().getColumnCount () ; i++ ) { %>
               <% String colName = result.getMetaData ().getColumnName ( i );
                  if ( colName.equalsIgnoreCase ( "pk" ) )
                     continue; %>
               <td bgcolor="lightgray" width="1"/>
               <td><center><I><%=colName%></I></center></td>
               <%}%>
               <td bgcolor="lightgray" width="1"/>
            </tr>
            <% long primaryKey = 0;
               while ( result.next () ) {
            %>
            <tr>
               <td bgcolor="lightgray" width="1"/>
               <%
                  primaryKey = result.getLong ( "PK" );
                  for ( int i = 2; i <= result.getMetaData ().getColumnCount () ; i++ ) { %>
               <% if ( result.getMetaData ().getColumnType ( i ) == Types.VARBINARY  || result.getMetaData ().getColumnType ( i ) == Types.BINARY || result.getMetaData ().getColumnType ( i ) == Types.BLOB || result.getMetaData ().getColumnType ( i ) == Types.LONGVARBINARY) { %>
               <td><center>
                  <%
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
                  %>
                  <% if (mimeType ==null) {%>
                  Not Specificed
                  <%} else if (mimeType.equalsIgnoreCase ( "jpeg")||mimeType.equalsIgnoreCase ( "png")||mimeType.equalsIgnoreCase ( "jpg")|| mimeType.equalsIgnoreCase ( "gif")) {%>
                  <IMG SRC="field?vs=<%=vsName%>&identity=<%=primaryKey%>&field=<%=result.getMetaData ().getColumnName ( i )%>">
                  <%} else if (mimeType.equalsIgnoreCase ( "svg")) {%>
                   <EMBED type="image/svg+xml" width="400" height="400" src="field?vs=<%=vsName%>&identity=<%=primaryKey%>&field=<%=result.getMetaData ().getColumnName ( i )%>&type=svg"   PLUGINSPAGE="http://www.adobe.com/svg/viewer/install/" />
                  <%} %>
               </center></td>
               <%} else if ( result.getMetaData ().getColumnName ( i ).toLowerCase().startsWith("time") ) {%>
               <td><center>
                  <%=new Time ( Long.parseLong ( result.getString ( i ) ) )%>.<%=(Long.parseLong ( result.getString ( i ) )%1000)%>
               </center></td>
               <%} else if ( result.getMetaData ().getColumnName ( i ).equalsIgnoreCase ( "PK" ) ) {
                  continue;
               } else { %>
               <td><center><%=result.getString ( i )%> <% } %></center></td>
               <td bgcolor="lightgray" width="1"/>
               <% } %>
            </tr>
            <%}%>
            <tr>
               <td colspan="<%=(result.getMetaData().getColumnCount()+1)*2+1%>"
                   bgcolor="lightgray">
                  <center><I>Pages</I></center>
               </td>
            </tr>
         </table>
      </center>
   </td>
</tr>
<%  StorageManager.getInstance ().returnResultSet ( result );  %>
<jsp:include page="footer.html"/>