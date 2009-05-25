package gsn;

import gsn.beans.DataField;
import gsn.beans.DataType;
import gsn.beans.StreamElement;
import gsn.beans.model.Parameter;
import gsn.beans.model.ParameterModel;
import gsn.sliding.CountBasedSliding;
import gsn.sliding.SlidingInterface;
import gsn.storage.StorageManager;
import gsn.utils.EasyParamWrapper;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.*;

import java.io.Serializable;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class TestSlidings {
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
        SlidingInterface sliding = new CountBasedSliding();
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
        MockSlidingListener listener = new MockSlidingListener();
        assertTrue(sliding.initialize(new EasyParamWrapper(params), listener));
        int count = 5;
        for (int i = 0; i < count; i++)
            sliding.postData(new StreamElement(new DataField[]{new DataField("val", "String", "")}, new Serializable[]{i}));
        assertEquals(listener.getSlidingCount(), 2);
    }
}
