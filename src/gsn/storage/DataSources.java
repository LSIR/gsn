package gsn.storage;

import gsn.storage.hibernate.DBConnectionInfo;
import gsn.utils.jndi.GSNContext;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;

public class DataSources {

    private static final transient Logger logger = Logger.getLogger( DataSources.class );

    public static BasicDataSource getDataSource(DBConnectionInfo dci) {
        BasicDataSource ds = null;
        try {
            ds = (BasicDataSource)GSNContext.getMainContext().lookup(Integer.toString(dci.hashCode()));
            if (ds == null) {
                ds = new BasicDataSource();
                /*
                BasicDataSource ds = new BasicDataSource();
                ds.setDriverClassName(driverClass);
                ds.setUsername(userName);
                ds.setPassword(password);
                ds.setUrl(url);

                ds.setMaxActive(maxActive);
                ds.setMaxIdle(maxIde);
                ds.setMaxWait(maxWait);
                ds.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
                ds.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
                */
                ds.setDriverClassName(dci.getDriverClass());
                ds.setUsername(dci.getUserName());
                ds.setPassword(dci.getPassword());
                ds.setUrl(dci.getUrl());
                ds.setMaxActive(8);
                ds.setMaxIdle(8);
                ds.setAccessToUnderlyingConnectionAllowed(true);
                GSNContext.getMainContext().bind(Integer.toString(dci.hashCode()), ds);
            }
        } catch (NamingException e) {
            logger.error(e.getMessage(), e);
        }
        return ds;
    }
}
