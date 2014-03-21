/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/TestDataPropogation.java
*
* @author gsn_devs
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.easymock.classextension.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.InputStream;
import gsn.beans.StreamElement;
import gsn.beans.StreamSource;
import gsn.storage.StorageManager;
import gsn.storage.StorageManagerFactory;
import gsn.wrappers.MockWrapper;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDataPropogation {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		DriverManager.registerDriver( new org.h2.Driver( ) );
		sm = StorageManagerFactory.getInstance ( "org.hsqldb.jdbcDriver","sa","" ,"jdbc:hsqldb:mem:.", Main.DEFAULT_MAX_DB_CONNECTIONS);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private static StorageManager sm;
	private MockWrapper wrapper;
	private StreamSource streamSource;
	
	@Before
	public void setUp() throws Exception {
		Properties p = new Properties();
		p.put("mock-test", "gsn.wrappers.MockWrapper");
	//		Main.loadWrapperList(propertiesConfiguration);
		VSensorLoader loader = new VSensorLoader();
		AddressBean addressBean= new AddressBean("mock-test");
		wrapper = (MockWrapper) loader.createWrapper(addressBean);
		InputStream is = new InputStream();
		streamSource= createMock(StreamSource.class, new Method[] {StreamSource.class.getMethod("windowSlided",new Class[] {})});
		streamSource.setAlias("test");
		streamSource.setRawHistorySize("1");
		streamSource.setAddressing(new AddressBean[] {addressBean});
		streamSource.setInputStream(is);
		streamSource.setSamplingRate(1);
		streamSource.setSqlQuery("select * from wrapper where data <> 1");
		assertTrue(loader.prepareStreamSource(streamSource,wrapper.getOutputFormat(),wrapper));
		assertNotNull(streamSource.toSql());
		assertNotNull(streamSource.getUIDStr());
		assertEquals(wrapper.getListeners().size(),1);
		is.setQuery("select * from test");
		is.setSources(streamSource);
		assertTrue(is.validate());
	}

	@After
	public void tearDown() throws Exception {
	 wrapper.removeListener(streamSource);
	 wrapper.releaseResources();
	}

	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#postStreamElement(gsn.beans.StreamElement)}.
	 * Testing to fix the duplication in the calls for the virtual sensor.
	 * We add one stream element which satisfies the query condition and we expect to receive true indicating
	 * that one of the clients should be notified.
	 * 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws SQLException 
	 */
	@Test
	public void testPostOneStreamElement() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, SQLException {
		StreamElement se = new StreamElement(streamSource.getWrapper().getOutputFormat(),new Serializable[] {10},System.currentTimeMillis());
		expect(streamSource.windowSlided()).andStubReturn(true);
		replay(streamSource);
		assertTrue(streamSource.validate());
		assertTrue(wrapper.insertIntoWrapperTable(se));
		assertEquals(sm.executeUpdate(new StringBuilder("delete from "+wrapper.getDBAliasInStr()+ " where TIMED="+se.getTimeStamp())),1);
		
		assertTrue(wrapper.publishStreamElement(se));
		verify(streamSource);
	}
	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#postStreamElement(gsn.beans.StreamElement)}.
	 * Testing to fix the duplication in the calls for the virtual sensor.
	 * We add two stream element which are satisfying the query condition and we expect to receive (two) true indicating
	 * that one of the clients should be notified.
	 * 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws SQLException 
	 */
	@Test
	public void testPostTwoStreamElements() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, SQLException {
		StreamElement se1 = new StreamElement(streamSource.getWrapper().getOutputFormat(),new Serializable[] {9},System.currentTimeMillis());
		StreamElement se2 = new StreamElement(streamSource.getWrapper().getOutputFormat(),new Serializable[] {10},System.currentTimeMillis()+10);
		expect(streamSource.windowSlided()).andReturn(true).times(2);
		replay(streamSource);
		assertTrue(streamSource.validate());
		assertTrue(wrapper.publishStreamElement(se1));
		assertTrue(wrapper.publishStreamElement(se2));
		verify(streamSource);
	}
	/**
	 * Test method for {@link gsn.wrappers.AbstractWrapper#postStreamElement(gsn.beans.StreamElement)}.
	 * Testing to fix the duplication in the calls for the virtual sensor.
	 * We add two stream element which are satisfying the query condition and we expect to receive (two) true indicating
	 * that one of the clients should be notified.
	 * 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws NoSuchMethodException 
	 * @throws SecurityException 
	 * @throws SQLException 
	 */
	@Test
	public void testPostTwoStreamElementsDropOne() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, SQLException {
		DataField[] df= streamSource.getWrapper().getOutputFormat();
		StreamElement se1 = new StreamElement(df,new Serializable[] {9},System.currentTimeMillis());
		StreamElement se2 = new StreamElement(df,new Serializable[] {10},System.currentTimeMillis()+10);
		StreamElement se3 = new StreamElement(df,new Serializable[] {1},System.currentTimeMillis()+11);
		expect(streamSource.windowSlided()).andReturn(true).times(2);
		replay(streamSource);
		assertTrue(streamSource.validate());
		assertTrue(wrapper.publishStreamElement(se1));
		assertTrue(wrapper.publishStreamElement(se2));
		assertFalse(wrapper.publishStreamElement(se3));
		assertTrue(streamSource.toSql().toString().toLowerCase().indexOf("mod")<0);
		verify(streamSource);
	}
}
