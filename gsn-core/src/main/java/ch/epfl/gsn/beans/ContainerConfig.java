/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
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
* File: src/ch/epfl/gsn/beans/ContainerConfig.java
*
* @author gsn_devs
* @author Ali Salehi
* @author Behnaz Bostanipour
* @author Timotee Maret
* @author Julien Eberle
*
*/

package ch.epfl.gsn.beans;

import ch.epfl.gsn.config.GsnConf;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import ch.epfl.gsn.beans.BeansInitializer;
import ch.epfl.gsn.beans.ContainerConfig;
import ch.epfl.gsn.beans.SlidingConfig;
import ch.epfl.gsn.beans.StorageConfig;
import ch.epfl.gsn.utils.KeyValueImp;


public class ContainerConfig {
	

	public static final String [ ]      JDBC_SYSTEMS                       = { "H2 in Memory" , "H2 in File" , "MySql", "SQL Server" };
	public static final String [ ]      JDBC_URLS                          = new String [ ] { "jdbc:h2:mem:." , "jdbc:h2:file:/path/to/file" , "jdbc:mysql://localhost:3306/ch.epfl.gsn", "jdbc:jtds:sqlserver://localhost/ch.epfl.gsn" };
	public static final String [ ]      JDBC_DRIVERS                       = new String [ ] { "org.h2.Driver" , "org.h2.Driver" , "com.mysql.jdbc.Driver", "net.sourceforge.jtds.jdbc.Driver" };
	public static final String [ ]      JDBC_URLS_PREFIX                   = new String [ ] { "jdbc:h2:mem:" , "jdbc:h2:file:" , "jdbc:mysql:", "jdbc:jtds:sqlserver:" };

	public static final String            NOT_PROVIDED                     = "Not Provided";
	
	public static final int               DEFAULT_MONITOR_PORT             = 22001;
	public static final int               DEFAULT_ZMQ_PROXY_PORT           = 22022;
	public static final int               DEFAULT_ZMQ_META_PORT            = 22023;
	public static final boolean           DEFAULT_ZMQ_ENABLED               = false;
    
	public static final String            FIELD_NAME_monitorPortNo         = "monitorPort";
	public static final String            FIELD_NAME_zmqEnabled            = "zmqEnabled";
	public static final String            FIELD_NAME_zmqProxyPort          = "zmqProxyPort";
	public static final String            FIELD_NAME_zmqMetaPort           = "zmqMetaPort";
	public static final String            FIELD_NAME_databaseSystem        = "databaseSystem";

	
	protected int                         monitorPort                      = DEFAULT_MONITOR_PORT;
	protected boolean                     zmqEnabled                       = DEFAULT_ZMQ_ENABLED;
	protected int                         zmqProxyPort                     = DEFAULT_ZMQ_PROXY_PORT;
	protected int                         zmqMetaPort                      = DEFAULT_ZMQ_META_PORT;
	protected String                      containerFileName;
	protected int                         storagePoolSize                  = -1;

    private StorageConfig                 storage ;
    private SlidingConfig                 sliding;
	private String                        gsnConfigurationFileName;
	private String                        databaseSystem;
	private boolean                       isdatabaseSystemInitialzied      = false;
	protected String                      timeFormat                       = "";

	public ContainerConfig(){
		
	}
	
	public ContainerConfig(int port, String timeFormat, boolean zmqEnabled,int zmqProxyPort,int zmqMetaPort,StorageConfig storage,SlidingConfig slide){
		this.monitorPort=port;
		this.timeFormat=timeFormat;
		this.zmqEnabled=zmqEnabled;
		this.zmqProxyPort=zmqProxyPort;
		this.zmqMetaPort=zmqMetaPort;
		this.storage=storage;
		this.sliding=slide;				
				
				
	}

    public StorageConfig getStorage() {
        return storage;
    }

    public SlidingConfig getSliding() {
        return sliding;
    }

    public String getContainerFileName ( ) {
		return this.containerFileName;
	}

	public void setContainerConfigurationFileName ( final String containerFileName ) {
		this.containerFileName = containerFileName;
	}


	public int getMonitorPort ( ) {
		return this.monitorPort;
	}
	
	public void setMonitorPort ( int newValue ) {
		this.monitorPort = newValue;
	}
	
	/**
	 * @return true if the zmq data distribution is enabled.
	 */
	public boolean isZMQEnabled() {
		return this.zmqEnabled;
	}
	
	/**
	 * @return Returns the ZeroMQ stream proxy port.
	 */
	public int getZMQProxyPort ( ) {
		return this.zmqProxyPort;
	}
	
	/**
	 * @return Returns the ZeroMQ meta information port.
	 */
	public int getZMQMetaPort ( ) {
		return this.zmqMetaPort;
	}

	/**
	 * @return Returns the storagePoolSize.
	 */
	public int getStoragePoolSize ( ) {
		return this.storagePoolSize;
	}

	public static ContainerConfig getConfigurationFromFile (String containerConfigurationFileName) throws FileNotFoundException {
		GsnConf gsn=GsnConf.load(containerConfigurationFileName);
		ContainerConfig toReturn = BeansInitializer.container(gsn);
		toReturn.setSourceFiles(containerConfigurationFileName);
		return toReturn;
	}

	private void setSourceFiles ( String gsnConfigurationFileName) {
		this.gsnConfigurationFileName = gsnConfigurationFileName;
	}

	public void setdatabaseSystem ( String newValue ) {
		isdatabaseSystemInitialzied = true;
		databaseSystem = newValue;
        storage = new StorageConfig();
        storage.setJdbcDriver(convertToDriver( newValue ));
        if ( newValue == JDBC_SYSTEMS[ 0 ] ) {
			storage.setJdbcPassword("");
            storage.setJdbcUsername("sa");
            storage.setJdbcURL(JDBC_URLS[ 0 ]);
		} else if ( newValue == JDBC_SYSTEMS[ 1 ] ) {
			storage.setJdbcPassword("");
            storage.setJdbcUsername("sa");
            storage.setJdbcURL(JDBC_URLS[ 1 ]);
		} else if ( newValue == JDBC_SYSTEMS[ 2 ] ) {
			storage.setJdbcURL(JDBC_URLS[ 2 ]);
		} else if ( newValue == JDBC_SYSTEMS[ 3 ] ) {
			storage.setJdbcURL(JDBC_URLS[ 3 ]);
		}
	}

	public String getdatabaseSystem ( ) {
		if ( isdatabaseSystemInitialzied == false ) {
			isdatabaseSystemInitialzied = true;

			for ( int i = 0 ; i < JDBC_URLS_PREFIX.length ; i++ )
				if ( storage.getJdbcURL().toLowerCase( ).trim( ).startsWith( JDBC_URLS_PREFIX[ i ] ) ) {
					setdatabaseSystem( JDBC_SYSTEMS[ i ] );
					break;
				}
		}
		return this.databaseSystem;
	}

	private String convertToDriver ( String dbSys ) {
		for ( int i = 0 ; i < JDBC_SYSTEMS.length ; i++ )
			if ( JDBC_SYSTEMS[ i ].equals( dbSys ) ) return JDBC_DRIVERS[ i ];
		return "";
	}

	public void writeConfigurations ( ) throws FileNotFoundException , IOException {
		StringTemplateGroup templateGroup = new StringTemplateGroup( "ch.epfl.gsn" );
		StringTemplate st = templateGroup.getInstanceOf( "ch.epfl.gsn/gui/templates/templateConf" );
		st.setAttribute( "db_user" , storage.getJdbcUsername( ) );
		st.setAttribute( "db_password" , storage.getJdbcPassword( ) );
		st.setAttribute( "db_driver" , storage.getJdbcDriver( ) );
		st.setAttribute( "db_url" , storage.getJdbcURL( ) );

		FileWriter writer = new FileWriter( gsnConfigurationFileName );
		writer.write( st.toString( ) );
		writer.close( );

	}

	public static ContainerConfig getDefaultConfiguration ( ) {
		ContainerConfig bean = new ContainerConfig( );
		bean.storage = new StorageConfig();
        bean.storage.setJdbcDriver( ContainerConfig.JDBC_SYSTEMS[ 0 ] );
		bean.storage.setJdbcPassword( "" );
		bean.storage.setJdbcURL( "sa" );
		bean.storage.setJdbcURL( ContainerConfig.JDBC_URLS[ 0 ] );
		return bean;
	}

	
	/**
	 * MSR MAP PART.
	 */
	private ArrayList<KeyValueImp> msrMap ;
	private HashMap<String, String> msrMapCached ;
	public HashMap<String, String> getMsrMap() {
		if (msrMapCached==null) {
			msrMapCached = new HashMap<String, String>();
			if (msrMap==null)
				return msrMapCached;
			for (KeyValueImp kv : msrMap)
				msrMapCached.put(kv.getKey().toLowerCase().trim(), kv.getValue());
		}
		return msrMapCached;
	}

	public String getTimeFormat() {
		return timeFormat;
	}
	
}
