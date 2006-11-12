<%@ page import="gsn.storage.*,gsn.notifications.*,gsn.web.*,gsn.vsensor.*,gsn.*,java.util.*" %>
<html>
	<body >
<%  final String task = request.getParameter ( "task" );
    final Container container = Mappings.getContainer ( ) ;
	if ( task != null &&task.trim ( ).equalsIgnoreCase ( "remove" ) ) { // REMOVE ACTION
         String notificationCode = request.getParameter ( "notificationCode" );
         boolean validNotificationCode = ( notificationCode != null ) && ( notificationCode.trim ( ).length ( ) > 0 ) ;
         if (  validNotificationCode ) {
            DummyNotification dummyNotification = new DummyNotification ( request.getParameter ( "notificationCode" ) ) ;
            container.removeNotificationRequest ( dummyNotification ) ;
         }
      }else if ( task!=null && task.trim ( ).equalsIgnoreCase ( "add" ) ) { // ADD Action
          String address =request.getParameter ( "address" ); 
          String message = request.getParameter ( "message" );
          String query =request.getParameter ( "query" );
          boolean validRequest = query!= null && query.trim ( ).length ( ) > 0 && message!= null && message.trim ( ).length ( ) > 0 && address  != null && address.trim ( ).length ( ) > 0 ;
          if ( validRequest) {
            NotificationRequest notificationReq = NotificationUtils.getProcessedQuery ( address,query,message);
            if ( notificationReq != null ) {
               ArrayList < String > virtualSensorNames = SQLUtils.extractTableNamesUsedInQuery ( notificationReq.getQuery ( ) ) ;
               for ( String virtualSensorName : virtualSensorNames ) 
                    container.addNotificationRequest ( virtualSensorName , notificationReq ) ;
            }
         }
      }
      NotificationRequest []  notificationRequests = container.getAllNotificationRequests ( )  ;
 %>



<%  %>
<tr height="90%">
<td>
<center>
<h1>Other Notification Services</h1>

<h3><p>In here you can register your device with the sensorIdentity network<br>
   using variety of notification protocol</p></h3>
<br>
<table align="center" cellpadding="0" cellspacing=0 width=400 border="0">
   <tr height="1">
      <td bgcolor="gray" colspan="5"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4><i>Protocol</i></h4></td>
      <td bgcolor="gray" width="1"/>
      <td><h4><i>Example</i></h4></td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr height="1">
      <td bgcolor="gray" colspan="5"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>sms</h4></td>
      <td bgcolor="gray" width="1"/>
      <td>sms://0761234567</td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>email</h4></td>
      <td bgcolor="gray" width="1"/>
      <td>email://xyz@foo.com</td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>fax</h4></td>
      <td bgcolor="gray" width="1"/>
      <td>fax://0212345678</td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>gsn</h4></td>
      <td bgcolor="gray" width="1"/>
      <td>gsn://123.123.123.123:1234</td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>pager</h4></td>
      <td bgcolor="gray" width="1"/>
      <td>pager://076123567</td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr height="1">
      <td bgcolor="gray" colspan="5"/>
   </tr>
</table>
<br>

<h3>Currently registered requests :</h3>
<% for ( NotificationRequest notificationRequest : notificationRequests ) {
   final int SMS = 1;
   final int EMAIL = 2;
   final int GSN = 3;
   final int FAX = 4;
   final int PAGER = 5;
   int type = 0;
   if ( notificationRequest instanceof GSNNotification )
      type = GSN;
   else if ( notificationRequest instanceof SMSNotification )
      type = SMS;
   else if ( notificationRequest instanceof EmailNotification )
      type = EMAIL;
%>
<br>
<table align="center" cellpadding="0" cellspacing=0 width=400 border="0">
   <tr height="1">
      <td bgcolor="gray" colspan="5"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td colspan="3"><h4>Registerd Query for <i>
         <%  StringBuffer result = new StringBuffer ();
            for ( String name : notificationRequest.getPrespectiveVirtualSensors () )
               result.append ( name ).append ( "," );
            result.deleteCharAt ( result.length () - 1 );
            out.print ( result.toString () );
         %></i></h4></td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr height="1">
      <td bgcolor="gray" colspan="5"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>Type</h4></td>
      <td bgcolor="gray" width="1"/>
      <td><%
         switch ( type ) {
            case SMS:
               out.print ( "SMS" );
               break;
            case GSN:
               out.print ( "GSN" );
               break;
            case EMAIL:
               out.print ( "Email" );
               break;
            default :
               out.print ( "Unknown" );
         }
      %></td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>Address</h4></td>
      <td bgcolor="gray" width="1"/>
      <td><%
         switch ( type ) {
            case SMS:
               out.print ( "---" );
               break;
            case GSN:
               out.print ( ( ( GSNNotification ) notificationRequest ).getRemoteAddress () + ":" + ( ( GSNNotification ) notificationRequest ).getRemotePort () );
               break;
            case EMAIL:
               out.print ( ( ( EmailNotification ) notificationRequest ).getReceiverEmailAddress () );
               break;
            default :
               out.print ( "Unknown" );
         }
      %></td>

      <td bgcolor="gray" width="1"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td><h4>Query</h4></td>
      <td bgcolor="gray" width="1"/>
      <td><%=notificationRequest.getQuery ()%></td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr height="1">
      <td bgcolor="gray" colspan="5"/>
   </tr>
   <tr>
      <td bgcolor="gray" width="1"/>
      <td colspan="3">
         <center>
            <form action="services.jsp" method="post">
               <input type="hidden" name="notificationCode" value="<%=notificationRequest.getNotificationCode()%>">
               <input type="hidden" name="task" value="remove">
               <input type="submit" value="Remove it"/>
            </form>
         </center>
      </td>
      <td bgcolor="gray" width="1"/>
   </tr>
   <tr height="1">
      <td bgcolor="gray" colspan="4"/>
   </tr>
</table>
<%}%>

<br>

<form action="/services.jsp" method="post">
   <table align="center" cellpadding="0" cellspacing=0 width=400 border="0">
      <tr height="1">
         <td bgcolor="gray" colspan="4"/>
      </tr>
      <tr>
         <td width="1" bgcolor="gray"/>
         <td colspan=2><center>Please fill the below form</center></td>
         <td width="1" bgcolor="gray"/>
      </tr>
      <tr height="1">
         <td colspan="4" height="1" bgcolor="gray"/>
      </tr>
      <tr height="8">
         <td width="1" bgcolor="gray"/>
         <td colspan=2/>
         <td width="1" bgcolor="gray"/>
         <tr>
            <td width="1" bgcolor="gray"/>
            <td>Address : </td>
            <td><input type="text" name="address" size="40"/></td>
            <td width="1" bgcolor="gray"/>
         </tr>
         <tr height="8">
            <td width="1" bgcolor="gray"/>
            <td colspan=2/>
            <td width="1" bgcolor="gray"/>
         </tr>
         <tr>
            <td width="1" bgcolor="gray"/>
            <td valign="top">Query : </td>
            <td><textarea name="query" rows="4" cols="40"></textarea></td>
            <td width="1" bgcolor="gray"/>
         </tr>
      <tr height="8">
         <td width="1" bgcolor="gray"/>
         <td colspan=2/>
         <td width="1" bgcolor="gray"/>
         <tr>
            <td width="1" bgcolor="gray"/>
            <td valign="top">Message : </td>
            <td><textarea name="message" rows="4" cols="40"></textarea></td>
            <td width="1" bgcolor="gray"/>
         </tr>
         <tr height="10">
            <td width="1" bgcolor="gray"/>
            <td colspan=2/>
            <td width="1" bgcolor="gray"/>
         </tr>
      <tr>
         <td width="1" bgcolor="gray"/>
         <td colspan="2"><center>
            <input type="hidden" name="task" value="add">
            <input type="submit" value="Add"/>
            <input type="reset" value="Clear"/>
         </td>
         <td width="1" bgcolor="gray"/>
      <tr>
      <tr height="1">
         <td bgcolor="gray" colspan="4"/>
      </tr>
   </table>
</from>
<br>
<table border="0">
   <tr align="left">
      <td>
         <h4>
            Information about how to address the sensorIdentity(s) is in
            <a href="registeration.jsp">here</a>.<br>
            Information about the data structure of indivitual sensorIdentity is in
            <a href="structure.jsp">here</a>.<br>
            For searching recent observations by hosted sensors click
            <a href="search.jsp">here</a>.</h4>
      </td>

   </tr>
</table>
</center>

</td>
</tr>
</body>
</html>