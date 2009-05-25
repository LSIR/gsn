package gsn.storage;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestHStorageManage {

    @Test
    public void testGetSessionFactoryH2() {
        String dialect = "org.hibernate.dialect.H2Dialect";
        String driverClass = "org.h2.Driver";
        String url = "jdbc:h2:mem:gsn_mem_db";
        String username = "sa";
        String password = "";

        String entityMapping =
                "<!DOCTYPE hibernate-mapping PUBLIC\n" +
                        "\"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\n" +
                        "\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">" +
                        "<hibernate-mapping>\n" +
                        "\n" +
                        "    <class entity-name=\"Customer\">\n" +
                        "\n" +
                        "        <id name=\"id\"\n" +
                        "            type=\"long\"\n" +
                        "            column=\"ID\">\n" +
                        "            <generator class=\"native\"/>\n" +
                        "        </id>\n" +
                        "\n" +
                        "        <property name=\"name\"\n" +
                        "            column=\"NAME\"\n" +
                        "            type=\"string\"/>\n" +
                        "\n" +
                        "        <property name=\"address\"\n" +
                        "            column=\"ADDRESS\"\n" +
                        "            type=\"string\"/>\n" +
                        "\n" +
                        "    </class>\n" +
                        "    \n" +
                        "</hibernate-mapping>";


        SessionFactory sessionFactory = HStorageManager.getSessionFactory(dialect, driverClass, url, username, password, entityMapping);
        Session session = sessionFactory.getCurrentSession();

        Transaction tx = session.beginTransaction();

// Create a customer
        Map david = new HashMap();
        david.put("name", "David");

// Save both
        long id = (Long) session.save("Customer", david);
        tx.commit();


        assertEquals(id, 1L);


        session = sessionFactory.getCurrentSession();
        tx = session.beginTransaction();
        Map savedDavid = (Map) session.get("Customer", id);
        tx.commit();
        assertEquals(savedDavid.get("name"), "David");

        sessionFactory.close();
    }
}
