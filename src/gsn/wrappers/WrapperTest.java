package gsn.wrappers;

import gsn.beans.AddressBean;
import gsn.beans.InputStream;
import gsn.beans.StreamSource;
import gsn.control.VSensorLoader;
import gsn.storage.StorageManager;
import gsn.utils.CaseInsensitiveComparator;

import java.util.ArrayList;
import java.util.TreeMap;

import junit.framework.TestCase;

/**
 * Sample class showing how to test the wrapper without starting the GSN server.
 * TODO : This test fails for now
 * 
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class WrapperTest extends TestCase {

    protected void setUp() throws Exception {
	super.setUp();
	StorageManager storageManager = StorageManager.getInstance();
	storageManager.initialize("org.hsqldb.jdbcDriver", "sa", "",
		"jdbc:hsqldb:mem:.");
    }

    protected void tearDown() throws Exception {
	super.tearDown();
    }

    public void testDummyWrapper() {
	DummyDataProducer dataSource = new DummyDataProducer();
	InputStream inputStream = new InputStream();
	inputStream
		.setQuery("select X.* from (select * from \"myAli2as\") as X where X.data>50");
	inputStream.setInputStreamName("bla");
	ArrayList<StreamSource> sources = new ArrayList<StreamSource>();
	StreamSource streamSource = new StreamSource("myAli2as",
		"select * from \"wrapper\" ", "10mse", null);
	sources.add(streamSource);
	// inputStream.setSources ( sources );
	TreeMap context = new TreeMap(new CaseInsensitiveComparator());
	context.put(VSensorLoader.STREAM_SOURCE, streamSource);
	context.put("ADDRESS", new AddressBean("my-wr", null));
	context
		.put(VSensorLoader.STORAGE_MANAGER, StorageManager
			.getInstance());
	boolean canInitialzie = dataSource.initialize(context);
	DataListener dataListener = new DataListener();
	TreeMap dsMap = new TreeMap(new CaseInsensitiveComparator());
	dsMap.put(VSensorLoader.STORAGE_MANAGER, StorageManager.getInstance());
	dsMap.put(VSensorLoader.STREAM_SOURCE, streamSource);
	dsMap.put(VSensorLoader.INPUT_STREAM, inputStream);
	dataListener.initialize(dsMap);
	dataSource.addListener(dataListener);
	while (true)
	    ;

    }

}
