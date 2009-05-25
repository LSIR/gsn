package gsn.vsensor;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import gsn.Container;
import static org.testng.Assert.*;
import org.testng.annotations.*;

public class TestContainerImpl {

    @AfterMethod
    public void clean() {

    }

    @Test
    public void gettingListOfVirtualSensors() throws Exception {
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest("http://localhost:22001/gsn");
        request.setParameter("REQUEST", Container.REQUEST_LIST_VIRTUAL_SENSORS + "");
        WebResponse response = wc.getResponse(request);
        assertTrue(response.getResponseMessage().contains("<gsn>"));
        // assertNotNull( response.getHeaderField( Container.RESPONSE ) );
    }

    @Test
    public void oneShotQueryExecution() throws Exception {
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest("http://localhost:22001/gsn");
        request.setHeaderField("REQUEST", Container.REQUEST_ONE_SHOT_QUERY + "");
        request.setHeaderField("VS_QUERY", "select * from LocalSystemTime");
        WebResponse response = wc.getResponse(request);
        assertEquals(response.getHeaderField(Container.RESPONSE_STATUS), Container.REQUEST_HANDLED_SUCCESSFULLY);
        assertNull(response.getHeaderField(Container.RESPONSE));
    }

    @BeforeMethod
    public void setup() {

    }

    @BeforeClass
    public static void init() {

    }

    @AfterClass
    public static void cleanAll() {

    }

}
