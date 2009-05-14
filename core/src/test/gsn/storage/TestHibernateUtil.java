package gsn.storage;

import org.junit.Test;
import org.hibernate.Session;
import org.hibernate.Transaction;
import gsn.beans.model.ParameterModel;
import gsn.beans.DataType;

public class TestHibernateUtil {
    @Test
    public void testHibernateUtil(){
        Session session = HibernateUtil.getCurrentSession();

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("pm1");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("2");

        Transaction tx = session.beginTransaction();

        session.save(parameterModel);

        tx.commit();
    }
}
