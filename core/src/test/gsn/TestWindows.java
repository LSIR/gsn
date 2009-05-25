package gsn;


import gsn.beans.DataField;
import gsn.beans.DataType;
import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;
import gsn.beans.model.ParameterModel;
import gsn.storage.StorageManager;
import gsn.utils.EasyParamWrapper;
import gsn.windows.CountBasedWindow;
import gsn.windows.TimeBasedWindow;
import gsn.windows.WindowInterface;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import org.testng.annotations.*;

import java.io.Serializable;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class TestWindows {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        DriverManager.registerDriver(new org.h2.Driver());
        StorageManager.getInstance().init("jdbc:h2:mem:.");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }


    @BeforeMethod
    public void setUp() throws Exception {

    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test
    public void testCountBasedWindow() {
        WindowInterface window = new CountBasedWindow();
        List<Parameter> params = new ArrayList<Parameter>();

        ParameterModel sizeModel = new ParameterModel();
        sizeModel.setDataType(DataType.NUMERIC);
        sizeModel.setDefaultValue("1");
        sizeModel.setName("size");
        sizeModel.setOptional(false);

        Parameter size = new Parameter();
        size.setModel(sizeModel);
        size.setValue("2");

        params.add(size);
        Assert.assertTrue(window.initialize(new EasyParamWrapper(params)));

        window.postData(new StreamElement(new DataField[]{new DataField("val", "String", "")}, new Serializable[]{0}, 1));
        List<StreamElement> items = window.nextWindow(2);
        assertEquals(items.size(), 2); //window size is defined two above.
        assertEquals(items.get(0).getData()[0], 0);
        assertNull(items.get(1));

        window.reset();
        assertEquals(window.getTotalContent().size(), 0);

        int count = 5;
        for (int i = 0; i < count; i++)
            window.postData(new StreamElement(new DataField[]{new DataField("val", "String", "")}, new Serializable[]{i}, i + 1));
        assertEquals(count, window.getTotalContent().size());
        items = window.nextWindow(2);
        assertEquals(items.size(), 2); //window size is defined two above.
        assertEquals(items.get(0).getData()[0], 1);
        assertEquals(items.get(1).getData()[0], 0);

        assertEquals(window.getTotalContent().size(), 5);   // no items consumed.

        items = window.nextWindow(4);
        items = window.nextWindow(5);

        assertEquals(window.getTotalContent().size(), 2);

        assertEquals(items.size(), 2); //window size is defined two above.
        assertEquals(items.get(0).getData()[0], 4);
        assertEquals(items.get(1).getData()[0], 3);

//        assertNull(items.get(1));
    }


    @Test
    public void testTimeBasedWindow() {
        WindowInterface window = new TimeBasedWindow();
        List<Parameter> params = new ArrayList<Parameter>();

        ParameterModel sizeModel = new ParameterModel();
        sizeModel.setDataType(DataType.NUMERIC);
        sizeModel.setDefaultValue("5000");
        sizeModel.setName("size");
        sizeModel.setOptional(false);

        Parameter size = new Parameter();
        size.setModel(sizeModel);
        size.setValue("5000");

        params.add(size);
        assertTrue(window.initialize(new EasyParamWrapper(params)));

        int count = 8;
        for (int i = 1; i <= count; i++)
            window.postData(new StreamElement(new DataField[]{new DataField("val", "String", "")}, new Serializable[]{i * 1000}, i * 1000));
        assertEquals(count, window.getTotalContent().size());
        List<StreamElement> items = window.nextWindow(5500);
        assertEquals(items.size(), 5);
        assertEquals(items.get(0).getTimeStamp(), 5000L);
        assertEquals(items.get(1).getTimeStamp(), 4000L);
        assertEquals(items.get(2).getTimeStamp(), 3000L);
        assertEquals(items.get(3).getTimeStamp(), 2000L);
        assertEquals(items.get(4).getTimeStamp(), 1000L);
        assertEquals(window.getTotalContent().size(), 8);

        items = window.nextWindow(8000);
        assertEquals(window.getTotalContent().size(), 5);
        assertEquals(items.size(), 5);
        assertEquals(items.get(0).getTimeStamp(), 8000L);
        assertEquals(items.get(1).getTimeStamp(), 7000L);
        assertEquals(items.get(2).getTimeStamp(), 6000L);
        assertEquals(items.get(3).getTimeStamp(), 5000L);
        assertEquals(items.get(4).getTimeStamp(), 4000L);

        items = window.nextWindow(9000);

        assertEquals(items.size(), 4);
    }

}
