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
import gsn.sliding.SlidingInterface;
import gsn.sliding.CountBasedSliding;

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

public class TestSlidings {
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
        SlidingInterface sliding = new CountBasedSliding();
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
        MockSlidingListener listener = new MockSlidingListener();
        assertTrue(sliding.initialize(new EasyParamWrapper(params),listener));
        int count = 5;
        for (int i=0;i<count;i++)
            sliding.postData(new StreamElement(new DataField[]{new DataField("val","String","")},new Serializable[]{i}));
        assertEquals(2,listener.getSlidingCount());
    }
}
