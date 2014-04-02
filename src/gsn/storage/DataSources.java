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
* File: src/gsn/storage/DataSources.java
*
* @author Timotee Maret
*
*/

package gsn.storage;

import gsn.storage.hibernate.DBConnectionInfo;
import gsn.utils.jndi.GSNContext;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.Logger;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;

public class DataSources {

    private static final transient Logger logger = Logger.getLogger( DataSources.class );

    public static BasicDataSource getDataSource(DBConnectionInfo dci) {
        BasicDataSource ds = null;
        try {
            ds = (BasicDataSource)GSNContext.getMainContext().lookup(Integer.toString(dci.hashCode()));
            if (ds == null) {
                ds = new BasicDataSource();
                ds.setDriverClassName(dci.getDriverClass());
                ds.setUsername(dci.getUserName());
                ds.setPassword(dci.getPassword());
                ds.setUrl(dci.getUrl());
		//ds.setDefaultTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                //ds.setAccessToUnderlyingConnectionAllowed(true); 
                GSNContext.getMainContext().bind(Integer.toString(dci.hashCode()), ds);
                logger.warn("Created a DataSource to: " + ds.getUrl());
            }
        } catch (NamingException e) {
            logger.error(e.getMessage(), e);
        }
        return ds;
    }
}
