package gsn.tests;

import org.testng.annotations.Test;
import static org.testng.Assert.*;
import gsn.beans.StreamElement;
import gsn2.wrappers.WrapperConfig;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.utils.Parameter;
import gsn2.conf.Parameters;


public class TestStreamElement {


    @Test
    public void testCreatingStreamElementForEmptyWrapper(){
        WrapperConfig conf = new WrapperConfig("test-wrapper",new Parameters(new Parameter("name","id")));
        MockWrapper wrapper = new MockWrapper(conf,null);

        wrapper.setOutputStructure(new DataField[]{});
        StreamElement se = StreamElement.from(wrapper);
        assertEquals(se.getFieldNames().length,0);
        assertEquals(se.getTimeInMillis(),null);
        assertFalse(se.isTimestampSet());
        assertEquals(se.getValue("something"),null);
        se.set("timed",1234);
        assertTrue(se.isTimestampSet());
    }


    @Test
    public void testCreatingStreamElementFromWrapper(){
        WrapperConfig conf = new WrapperConfig("test-wrapper",new Parameters(new Parameter("name","id")));
        MockWrapper wrapper = new MockWrapper(conf,null);
        wrapper.setOutputStructure(new DataField[]{new DataField("field1","string")});
        StreamElement se = StreamElement.from(wrapper).setTime(System.currentTimeMillis());
        assertEquals(se.getFieldNames().length,1);
        assertEquals(se.getValue("field1"),null);
        se.set("field1",1234);
        assertEquals(se.getValue("field1"),1234);
        assertNotNull(se.getTimeInMillis());
        assertNotNull(se.getValue("timed"));
        assertEquals((int)se.getType("field1"), DataTypes.convertTypeNameToGSNTypeID("string"));
        assertNull(se.getType("field2"));
    }

    @Test
    public void testStreamElementIsEmpty(){
        WrapperConfig conf = new WrapperConfig("test-wrapper",new Parameters(new Parameter("name","id")));
        MockWrapper wrapper = new MockWrapper(conf,null);
        wrapper.setOutputStructure(new DataField[]{new DataField("field1","string")});
        StreamElement se = StreamElement.from(wrapper).setTime(System.currentTimeMillis());
        assertFalse(se.isEmpty());
        assertTrue(StreamElement.from(wrapper).isEmpty());
        assertFalse(StreamElement.from(wrapper).setTime(1234l).isEmpty());
    }

}
