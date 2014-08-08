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
* File: src/gsn/storage/SQLValidator.java
*
* @author Ali Salehi
* @author Timotee Maret
* @author Mehdi Riahi
* @author Julien Eberle
*
*/

package gsn.storage;

import gsn.VSensorStateChangeListener;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.h2.command.CommandInterface;
import org.h2.command.Parser;
import org.h2.command.Prepared;
import org.h2.command.dml.Select;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.Session;
import org.h2.engine.SessionFactoryEmbedded;
import org.h2.expression.Expression;
import org.h2.util.ObjectArray;

public class SQLValidator implements VSensorStateChangeListener {

	private static final transient Logger logger             = Logger.getLogger( SQLValidator.class );

	private Session session = null;
	private Connection connection;
	private static SQLValidator validator ;

	public synchronized static SQLValidator getInstance() throws SQLException {
		if (validator==null)
			validator = new SQLValidator();
		return validator  ;
	}

	private  SQLValidator() throws SQLException {
		Properties properties = new Properties();
		properties.put("user", "sa");
		properties.put("password","");
		String URL = "jdbc:h2:mem:test";
		ConnectionInfo connInfo = new ConnectionInfo(URL,properties);
		SessionFactoryEmbedded factory = new SessionFactoryEmbedded();
		session = (Session) factory.createSession(connInfo);
		this.connection = DriverManager.getConnection(URL,properties);
		
		//This is only a workaround for queries containing 'UNIX_TIMESTAMP()' with no parameter.
		//It does not return the same value as UNIX_TIMESTAMP() in MySQL returns!
		executeDDL("CREATE ALIAS UNIX_TIMESTAMP FOR \"java.lang.System.currentTimeMillis()\"");
	}

	public void executeDDL(String ddl) throws SQLException {
		CommandInterface command = session.prepareCommand(ddl, 0);
		command.executeUpdate();
	}

	/**
	 * Conditions to check.
	 * 1. No inner select.(done)
	 * 2. Be a select (done)
	 * 3. Valid fields (done)
	 * 4. Valid SQL (done)
	 * 5. Single Select without joins.(done)
	 * 6. Only using one table. (done)
	 * 7. No aggregation, groupby or having. (done)
	 * 8. no order by (done)
	 * 9. no limit keyword (done)
	 */
	public static String removeQuotes(String in) {
		return in.replaceAll("\"([^\"]|.)*\"", "");
	}
	public static String removeSingleQuotes(String in) {
		return in.replaceAll("'([^']|.)*'", "");
	}

	private static boolean isValid(String query) {
		String simplified = removeSingleQuotes(removeQuotes(query)).toLowerCase().trim();
		if (simplified.lastIndexOf("select") != simplified.indexOf("select"))
			return false;
		if (simplified.indexOf("order by") >0 || simplified.indexOf("group by") >0 || simplified.indexOf("having") >0 || simplified.indexOf("limit") >0 || simplified.indexOf(";") >0)
			return false;
		return true;
	}

	public static String addTopFirst(String query) {
		return query + " order by TIMED desc limit 1";
	}

	/**
	 * Returns null if the validation fails. Returns the table name used in the query if the validation succeeds.
	 * @param query to validate.
	 * @return Null if the validation fails. The name of the table if the validation succeeds.
	 */
	public  String validateQuery(String query) {
		Select select = queryToSelect(query);
		if (select ==null)
			return null;
		if ((select.getTables().size() != 1) || (select.getTopFilters().size()!=1) || select.isQuickAggregateQuery() ) 
			return null;
		return select.getTables().iterator().next().getName();
	}

	public  DataField[] extractSelectColumns(String query, VSensorConfig vSensorConfig) {
		Select select = queryToSelect(query);
		if (select ==null)
			return new DataField[0];
		
		return getFields(select,vSensorConfig.getOutputStructure());
	}
	
	//to allow the use of queries over models and not only VS
	public  DataField[] extractSelectColumns(String query, DataField[] datafields) {
		Select select = queryToSelect(query);
		if (select ==null)
			return new DataField[0];
		
		return getFields(select,datafields);
	}


	public Connection getSampleConnection() {
		return connection;
	}

	public boolean vsLoading(VSensorConfig config) {

		return false;
	}

	public boolean vsUnLoading(VSensorConfig config) {

		return false;
	}

	private DataField[] getFields(Select select, DataField[] fields) {
		ArrayList<DataField> toReturn = new ArrayList<DataField>();
		try {
			for (int i=0;i<select.getColumnCount();i++) {
				String name = select.queryMeta().getColumnName(i);
				if (name.equalsIgnoreCase("timed") || name.equalsIgnoreCase("pk") )
					continue;
				String gsnType = null;
				for (int j=0;j<fields.length;j++) {
					if (fields[j].getName().equalsIgnoreCase(name)) {	
						gsnType=fields[j].getType();
						toReturn.add( new DataField(name,gsnType));
						break;
					}
				}				
			}
			return toReturn.toArray(new DataField[] {});
		}catch (Exception e) {
			logger.debug(e.getMessage(),e);
			return new DataField[0];
		}
		
	}
	private Select queryToSelect(String query) {
		Select select  = null;
		if (!isValid(query))
			return null;
		Parser parser = new Parser(session);
		Prepared somePrepared;
		try {
			somePrepared = parser.parseOnly(query);
			if (somePrepared instanceof Select && somePrepared.isQuery()) 
				select = (Select) somePrepared;
		} catch (SQLException e) {
			logger.debug(e.getMessage(),e);
		}
		return select;
	}

    public static String addPkField(String query) {
        logger.debug("< QUERY IN: " + query);
        try {
            SQLValidator sv = getInstance();
            Select select = sv.queryToSelect(query);
            if (select == null)
                return query;
            boolean hasPk = false;
            boolean hasWildCard = false;
            for (int i=0;i<select.getColumnCount();i++) {
				String name = select.queryMeta().getColumnName(i);
				if (name.equalsIgnoreCase("*")) {
                    hasWildCard = true;
                    break;
                }
                if (name.equalsIgnoreCase("pk")) {
                    hasPk = true;
                    break;
                }
			}
            //
            if (! hasPk && !hasWildCard) {
                int is = query.toUpperCase().indexOf("SELECT");
                query = new StringBuilder(query.substring(is,is+6))
                        .append(" pk, ")
                        .append(query.substring(is+7)).toString();
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);    
        }
        logger.debug("> QUERY OUT: " + query);
        return query;
    }

	public void release() throws Exception {
		if(connection !=null && !connection.isClosed())
			connection.close();
		
	}

}
