package gsn.storage;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.tool.hbm2ddl.SchemaExport;

public class HibernateUtil {
    private static Logger logger = Logger.getLogger(HibernateUtil.class);
    private static SessionFactory sessionFactory;

    static {
        try {
            AnnotationConfiguration cfg = new AnnotationConfiguration().configure();
            SchemaExport schemaExport = new SchemaExport(cfg);
            schemaExport.setOutputFile("hibernate.sql");
            schemaExport.create(true, false);
            sessionFactory = cfg.buildSessionFactory();
        } catch (Throwable ex) {
            logger.error("Initial SessionFactory creation failed.", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static void shutdown() {
        sessionFactory.close();
    }

    public static Session getCurrentSession() throws HibernateException {
        return sessionFactory.getCurrentSession();
    }
}
