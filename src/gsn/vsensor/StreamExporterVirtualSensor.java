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
* File: src/gsn/vsensor/StreamExporterVirtualSensor.java
*
* @author Jerome Rousselot
* @author Ali Salehi
* @author Mehdi Riahi
* @author Timotee Maret
* @author Ivo Dimitrov
*
*/

package gsn.vsensor;

import gsn.Main;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.utils.GSNRuntimeException;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * This virtual sensor saves its input stream to any JDBC accessible source.
 */
public class StreamExporterVirtualSensor extends AbstractVirtualSensor {

	public static final String            PARAM_USER    = "user" , PARAM_PASSWD = "password" , PARAM_URL = "url" , TABLE_NAME = "table",PARAM_DRIVER="driver",PARAM_ENTRIES="entries";

	public static final String[] OBLIGATORY_PARAMS = new String[] {PARAM_USER,PARAM_URL,PARAM_DRIVER};

	private static final transient Logger logger        = Logger.getLogger( StreamExporterVirtualSensor.class );

	private Connection                    connection;

	private CharSequence table_name;

	private String password;

	private String user;
	
	private String url;

    private String entries;      //
    private long startTime;
    private long estimatedTime;
    private int counter = 0;
    private int limit;

	public boolean initialize ( ) {
		VSensorConfig vsensor = getVirtualSensorConfiguration( );
		TreeMap < String , String > params = vsensor.getMainClassInitialParams();

		for (String param : OBLIGATORY_PARAMS)
			if ( params.get( param ) == null || params.get(param).trim().length()==0) {
				logger.warn("Initialization Failed, The "+param+ " initialization parameter is missing");
				return false;
			}
		table_name = params.get( TABLE_NAME );
		user = params.get(PARAM_USER);
		password = params.get(PARAM_PASSWD);
		url = params.get(PARAM_URL);
        entries = params.get(PARAM_ENTRIES);   //
        limit = Integer.parseInt(entries);       //
        estimatedTime = 0;
		try {
			Class.forName(params.get(PARAM_DRIVER));
			connection = getConnection();
			logger.debug( "jdbc connection established." );
			if (!Main.getStorage(table_name.toString()).tableExists(table_name,getVirtualSensorConfiguration().getOutputStructure() , connection))
				Main.getStorage(table_name.toString()).executeCreateTable(table_name, getVirtualSensorConfiguration().getOutputStructure(), false,connection);
		} catch (ClassNotFoundException e) {
			logger.error(e.getMessage(),e);
			logger.error("Initialization of the Stream Exporter VS failed !");
			return false;
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
			logger.error("Initialization of the Stream Exporter VS failed !");
			return false;
		}catch (GSNRuntimeException e) {
			logger.error(e.getMessage(),e);
			logger.error("Initialization failed. There is a table called " + TABLE_NAME+ " Inside the database but the structure is not compatible with what GSN expects.");
			return false;
		}
		return true;
	}

	public void dataAvailable ( String inputStreamName , StreamElement streamElement ) {
		StringBuilder query = Main.getStorage(table_name.toString()).getStatementInsert(table_name, getVirtualSensorConfiguration().getOutputStructure());
		
		try {
            counter++;        //
            startTime = System.nanoTime();
            Main.getStorage(table_name.toString()).executeInsert(table_name ,getVirtualSensorConfiguration().getOutputStructure(),streamElement,getConnection() );
            estimatedTime += (System.nanoTime() - startTime);
            if (counter >= limit) {
                double seconds = (double)estimatedTime / 1000000000.0;
                System.out.println("The estimated time (sec) is = "+seconds);
		logger.warn("*** ESTIMATED TIME (SEC) IS "+seconds);
            }
	    if ((counter % 1000) == 0) {
                System.out.println("Up until the Entry = "+counter);
		double seconds = (double)estimatedTime / 1000000000.0;
                System.out.println("The estimated time (sec) is = "+seconds);
                logger.warn("*** ESTIMATED TIME (SEC) for counter = "+counter+" IS "+seconds);
            }
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
			logger.error("Insertion failed! ("+ query+")");
		}finally {
			dataProduced( streamElement );
		}
		
	}


	public Connection getConnection() throws SQLException {
		if (this.connection==null || this.connection.isClosed())
			this.connection=DriverManager.getConnection(url,user,password);
		return connection;
	}


	public void dispose ( ) {
	}
}
