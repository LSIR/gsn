package gsn.tests;


import gsn2.wrappers.WrapperConfig;
import gsn.utils.Parameter;
import gsn2.conf.Parameters;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;

public class TestAdressBean {
	private WrapperConfig addressBean1;
	private WrapperConfig addressBean2;
	
	@BeforeClass
	public static void init(){
		
	}

	@Test
	public void testHashCode() {
		Parameter[] predicates1 = new Parameter[2];
		Parameter[] predicates2 = new Parameter[2];
		predicates1[0] = new Parameter("key1", "val1");
		predicates2[0] = new Parameter("key1", "val1");
		
		predicates1[1] = new Parameter("key2", "val2");
		predicates2[1] = new Parameter("key2", "val2");
		addressBean1 = new WrapperConfig("wrapper", new Parameters(predicates1));
		addressBean2 = new WrapperConfig("wrapper", new Parameters(predicates2));
		assertEquals(addressBean1.hashCode(), addressBean2.hashCode());
		predicates1[0] = new Parameter("val1", "key1");
		assertTrue(addressBean1.hashCode() != addressBean2.hashCode());
		addressBean1 = new WrapperConfig("wrapper", new Parameters(new Parameter("key1", "key2")));
		assertTrue(addressBean1.hashCode() != addressBean2.hashCode());		
	}

	@Test
	public void testEqualsObject() {
		Parameter[] predicates1 = new Parameter[2];
		Parameter[] predicates2 = new Parameter[2];
		predicates1[0] = new Parameter("key1", "val1");
		predicates2[0] = new Parameter("key1", "val1");
		
		predicates1[1] = new Parameter("key2", "val2");
		predicates2[1] = new Parameter("key2", "val2");
		addressBean1 = new WrapperConfig("wrapper", new Parameters(predicates1));
		addressBean2 = new WrapperConfig("wrapper", new Parameters( predicates2));
		assertEquals(addressBean1, addressBean2);
		predicates1[0] = new Parameter("val1", "key1");
		assertFalse(addressBean1.equals(addressBean2));
		addressBean1 = new WrapperConfig("wrapper", new Parameters(new Parameter("key1", "key2")));
		assertFalse(addressBean1.equals(addressBean2));	
	}

}
