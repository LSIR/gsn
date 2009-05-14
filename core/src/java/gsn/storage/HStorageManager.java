package gsn.storage;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;


public class HStorageManager {

    public static SessionFactory getSessionFactory(String dialect, String driverClass, String url, String userName, String password, String entityMapping){
        Configuration cfg = new Configuration();
        cfg.setProperty("hibernate.connection.url", url);
        cfg.setProperty("hibernate.connection.username", userName);
        cfg.setProperty("hibernate.connection.password", password);
        cfg.setProperty("hibernate.connection.driver_class", driverClass);
        cfg.setProperty("hibernate.dialect", dialect);
        cfg.setProperty("hibernate.current_session_context_class", "thread");

        cfg.setProperty("hibernate.default_entity_mode", "dynamic-map");
        cfg.setProperty("hibernate.hbm2ddl.auto", "create");
        
        cfg.setProperty("cache.provider_class", "org.hibernate.cache.NoCacheProvider");
        cfg.setProperty("show_sql", "true");

        cfg.addXML(entityMapping);

        return cfg.buildSessionFactory();
    }

    public static void closeSessionFactory(SessionFactory sessionFactory){
        sessionFactory.close();
    }
}
