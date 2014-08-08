/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/storage/hibernate/HibernateUtil.java
*
* @author Timotee Maret
*
*/

package gsn.storage.hibernate;

import gsn.storage.DataSources;
import gsn.utils.jndi.GSNContext;
import gsn.utils.jndi.GSNContextFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.*;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaValidator;

public class HibernateUtil {

    private static final transient Logger logger = Logger.getLogger( HibernateUtil.class );

    

    /**
     * 
     * @param driverClass
     * @param url
     * @param userName
     * @param password
     * @param entityMapping
     * @return
     */
    public static SessionFactory getSessionFactory(String driverClass, String url, String userName, String password, String entityMapping) {

        DBConnectionInfo conn = new DBConnectionInfo(driverClass,url,userName,password);
        DataSources.getDataSource(conn);
        //
        Configuration cfg = new Configuration();
        cfg.setProperty("hibernate.current_session_context_class", "thread");
        cfg.setProperty("hibernate.default_entity_mode", "dynamic-map");
        cfg.setProperty("hibernate.connection.datasource",Integer.toString(conn.hashCode()));
        cfg.setProperty("hibernate.jndi.class", GSNContextFactory.class.getCanonicalName());
        cfg.setProperty("hibernate.show_sql", "false");
        cfg.setProperty("hibernate.format_sql", "true");
        cfg.addXML(entityMapping);
        //
        SessionFactory session = null;
        try {
            session = cfg.buildSessionFactory();
        }
        catch(Exception e) {
            logger.error("error: " + e.getMessage());
        }
        //
        cfg.setProperty("hibernate.dialect", ((SessionFactoryImplementor)session).getDialect().toString());
        // Create the table if it does not exist already.
        try {
            // script, export, justDrop, justCreate
            new SchemaExport(cfg).execute(false, true, false, true);
        }
        catch (HibernateException e) {
            logger.error(e.getMessage(), e);
        }
        // Check if the table exists and has the proper outputformat
        try {
            new SchemaValidator(cfg).validate();
        }
        catch (HibernateException e) {
            session = null;
            logger.error("Failed create the table: " + e.getMessage());
        }
        return session;
    }

    public static void closeSessionFactory(SessionFactory sessionFactory) {
        sessionFactory.close();
    }

}
