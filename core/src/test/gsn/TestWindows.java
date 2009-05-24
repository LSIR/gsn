package gsn;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import gsn.beans.*;
import gsn.beans.model.Parameter;
import gsn.beans.model.ParameterModel;
import gsn.windows.WindowInterface;
import gsn.windows.CountBasedWindow;
import gsn.storage.StorageManager;
import gsn.utils.EasyParamWrapper;

import java.io.Serializable;
import java.sql.DriverManager;
import java.util.List;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.testng.AssertJUnit.*;

public class TestWindows {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        DriverManager.registerDriver(new org.h2.Driver());
        StorageManager.getInstance().init("jdbc:h2:mem:.");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }


    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCountBasedWindow() {
        WindowInterface window = new CountBasedWindow();
        List<Parameter> params = new ArrayList();

        ParameterModel sizeModel = new ParameterModel();
        sizeModel.setDataType(DataType.NUMERIC);
        sizeModel.setDefaultValue("1");
        sizeModel.setName("size");
        sizeModel.setOptional(false);

        Parameter size = new Parameter();
        size.setModel(sizeModel);
        size.setValue("2");

        params.add(size);
        assertTrue(window.initialize(new EasyParamWrapper(params)));
        int count = 5;
        for (int i=0;i<count;i++)
            window.postData(new StreamElement(new DataField[]{new DataField("val","String","")},new Serializable[]{i}));
        assertEquals(count,window.getTotalContent().size());
        List<StreamElement> items = window.nextWindow();
        assertEquals(2,items.size()); //window size is defined two above.
        assertEquals(0,items.get(0).getData()[0]);
        assertEquals(1,items.get(1).getData()[0]);
        assertEquals(3,window.getTotalContent().size());   // two items consumed.
        items = window.nextWindow();
        items = window.nextWindow();
        assertEquals(0,window.getTotalContent().size());
        assertEquals(2,items.size()); //window size is defined two above.
        assertEquals(4,items.get(0).getData()[0]);
        assertNull(items.get(1));
    }


}
