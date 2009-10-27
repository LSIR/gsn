package gsn;

import gsn.beans.StreamElement;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

public class GSNRequestHandler {

  private static transient Logger logger = Logger.getLogger( GSNRequestHandler.class );

  public static String[][] parseOutputStructureFromREST(String output) {
    StringTokenizer st = new StringTokenizer(output,",",false);
    String[][] toReturn = new String[st.countTokens()][2] ;
    int i=0;
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      toReturn[i++] = token.split("=>");
    }
    return toReturn;
  }
/**
 * This method has to be modified to be adapted to httpclient/httpcore library version 4.0
 * @param query
 * @param remoteVSName
 * @param windowSize
 * @param slideValue
 * @param remoteHost
 * @param remotePort
 * @param localHost
 * @param localPort
 * @param notificationCode
 * @return
 */
  public static boolean sendRegisterQueryRequest(String query,String remoteVSName, String windowSize, String slideValue, String remoteHost,int remotePort,String localHost, int  localPort,int notificationCode) {
    String url = "http://"+remoteHost+":"+remotePort+"/gsn/register/"+remoteVSName+"/"+windowSize+"/"+slideValue+"/notify/"+localHost+"/"+localPort+"/"+notificationCode;
    HttpPost post = new HttpPost(url);
    HttpClient client = new DefaultHttpClient();
//    post.setEntity(new HttpEntity(new NameValuePair[] {new NameValuePair("query",query)});
    HttpResponse status = null;
    try {
      status = client.execute(post);
      System.out.println(status.toString());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return true;
//    return status.get == 201;

  }

  public static boolean deliverDataFromRest( int notificationCode ,  String [] fieldNamesInStreamElement ,  Object [] fieldValues ) {
//    long timeStamp = NumberUtils.toLong(timeStampStr ,0);
    
    AbstractWrapper remoteWrapper = null;// = Mappings.getContainer( ).findNotificationCodeListener( notificationCode );
    if ( remoteWrapper == null ) { // This client is no more interested in this notificationCode.
      if ( logger.isInfoEnabled( ) ) logger.info( "Invalid notification code >"+notificationCode+"<recieved, query droped." );
      return false;
    } else {
      // /**
      // * If the received stream element from remote stream producer
      // * doesn't have the timed field inside, the<br>
      // * container will automatically insert the receive time to the
      // * stream element.<br>
      // */
      StreamElement se = StreamElement.createElementFromREST(remoteWrapper.getOutputFormat( ), fieldNamesInStreamElement , fieldValues );
      if ( logger.isDebugEnabled( ) ) logger.debug( new StringBuilder( ).append( "data received for notification code *" ).append( notificationCode ).toString( ) );
      return remoteWrapper.manualDataInsertion(se);
    }
  }

}
