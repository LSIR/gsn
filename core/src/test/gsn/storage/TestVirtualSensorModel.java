package gsn.storage;

import org.junit.Test;
import org.hibernate.Session;
import org.hibernate.Transaction;
import gsn.beans.model.ParameterModel;
import gsn.beans.model.WindowModel;
import gsn.beans.model.VirtualSensor;
import gsn.beans.DataType;

import java.util.ArrayList;

public class TestVirtualSensorModel {

    private VirtualSensor createVirtualSensor(){
        VirtualSensor virtualSensor = new VirtualSensor();

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("pm1");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("2");
        

        return virtualSensor;
    }


    @Test
    public void testHibernateUtil(){
        Session session = HibernateUtil.getCurrentSession();

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("pm1");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("2");

        WindowModel windowModel = new WindowModel();
        windowModel.setName("name");
        windowModel.setDescription("description");
        windowModel.setClassName("a.b.C");
        ArrayList<ParameterModel> models = new ArrayList<ParameterModel>();
        models.add(parameterModel);
        windowModel.setParameters(models);

        Transaction tx = session.beginTransaction();

        session.save(parameterModel);
        session.save(windowModel);

        tx.commit();
    }
}
