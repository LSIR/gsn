package gsn.vsensor;

import static org.junit.Assert.*;
import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.oreilly.servlet.multipart.MultipartParser;

/**
 * @author alisalehi
 * This class assumes GSN is running on the localhost:22001.
 */
public class TestContainerImpl {
   @After public void clean() {
      
   }
   @Test public void gettingGeneralWebAppInformations() throws Exception {
      WebConversation wc = new WebConversation();
      WebRequest     request = new GetMethodWebRequest( "http://localhost:22001/gsn" );
      request.setHeaderField( "REQUEST" , Container.REQUEST_WEB_APP_INFORMATION+"" );
      WebResponse   response = wc.getResponse( request );
      assertEquals(response.getHeaderField( Container.RESPONSE_STATUS),Container.REQUEST_HANDLED_SUCCESSFULLY );
      assertNotNull( response.getHeaderField( Container.WEB_APP_AUTHOR ) );
      assertNotNull( response.getHeaderField( Container.WEB_APP_DESCRIPTION ) );
      assertNotNull( response.getHeaderField( Container.WEB_APP_EMAIL ) );
      assertNotNull( response.getHeaderField( Container.WEB_APP_NAME ) );
      
   }
   @Test public void gettingListOfVirtualSensors() throws Exception {
      WebConversation wc = new WebConversation();
      WebRequest     request = new GetMethodWebRequest( "http://localhost:22001/gsn" );
      request.setHeaderField( "REQUEST" , Container.REQUEST_LIST_VIRTUAL_SENSORS+"" );
      WebResponse   response = wc.getResponse( request );
      assertEquals(response.getHeaderField( Container.RESPONSE_STATUS),Container.REQUEST_HANDLED_SUCCESSFULLY );
      assertNotNull( response.getHeaderField( Container.RESPONSE ) );
   }
   @Test public void oneShotQueryExecution() throws Exception {
      WebConversation wc = new WebConversation();
      WebRequest     request = new GetMethodWebRequest( "http://localhost:22001/gsn" );
      request.setHeaderField( "REQUEST" , Container.ONE_SHOT_QUERY_EXECUTION_REQUEST+"" );
      request.setHeaderField( "VS_QUERY" , "select * from LocalSystemTime" );
      WebResponse   response = wc.getResponse( request );
      assertEquals(response.getHeaderField( Container.RESPONSE_STATUS),Container.REQUEST_HANDLED_SUCCESSFULLY );
      assertNull( response.getHeaderField( Container.RESPONSE ) );
   }
   @Before public void setup() {
      
   }
   @BeforeClass public static void init() {
      
   }
   @AfterClass public static void cleanAll() {
      
   }
  
   public static junit.framework.Test suite() {
      return new JUnit4TestAdapter(TestContainerImpl.class);
   }
   
}
