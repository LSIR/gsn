/**
 * 
 * @author Jerome Rousselot
 */
package gsn.vsensor;

import static org.junit.Assert.*;

import gsn.beans.VSensorConfig;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * @author jerome
 *
 */
public class TestStreamExporterVirtualSensor extends TestCase {
	
	HashMap hashMap;
	
	@Before public void setUp() {
		hashMap = new HashMap();
		hashMap.put(VirtualSensorPool.CONTAINER, new ContainerImpl());
		VSensorConfig config = new VSensorConfig();
		config.setVirtualSensorName("JUnitTestStreamExporterVS");
		config.setFileName("PlaceholderfileNameForJUNitTesting");
		config.setAuthor("Jerome Rousselot");
		hashMap.put(VirtualSensorPool.VSENSORCONFIG, config);
	}
	
	@After public void tearDown() {
		hashMap = null;
	}
	
	@Test public void testMissingAllEssentialParameters() {
		StreamExporterVirtualSensor vs = new StreamExporterVirtualSensor();	
		assertFalse(vs.initialize(hashMap));
	}
	
	@Test public void testCreateNewTableInExistingDB() {
		
		assertTrue(true);
	}

	
}
