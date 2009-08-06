package gsn.storage;

import gsn.VSensorStateChangeListener;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.VSFile;

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
import org.h2.value.DataType;

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
		String URL = "jdbc:h2:mem:validator";
		ConnectionInfo connInfo = new ConnectionInfo(URL,properties);
		SessionFactoryEmbedded factory = new SessionFactoryEmbedded();
		session = (Session) factory.createSession(connInfo);
		this.connection = DriverManager.getConnection(URL,properties);
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

	public  DataField[] extractSelectColumns(String query) {
		Select select = queryToSelect(query);
		if (select ==null)
			return new DataField[0];
		return getFields(select);
	}

	public Session getSession() {
		return session;
	}

	public Connection getSampleConnection() {
		return connection;
	}

	public boolean vsLoading(VSFile config) {

		return false;
	}

	public boolean vsUnLoading(VSFile config) {

		return false;
	}

	private DataField[] getFields(Select select) {
		ArrayList<DataField> toReturn = new ArrayList<DataField>();
		try {
			for (int i=0;i<select.getColumnCount();i++) {
				String name = select.queryMeta().getColumnName(i);
				if (name.equalsIgnoreCase("timed") || name.equalsIgnoreCase("pk") )
					continue;
				byte gsnType = DataTypes.SQLTypeToGSNTypeSimplified(DataType.convertTypeToSQLType(select.queryMeta().getColumnType(i)));
				toReturn.add( new DataField(name,gsnType));
			}
			return toReturn.toArray(new DataField[] {});
		}catch (Exception e) {
			logger.warn(e.getMessage(),e);
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
			logger.warn(e.getMessage(),e);
		}
		return select;
	}

	public void release() throws Exception {
		if(connection !=null && !connection.isClosed())
			connection.close();
		
	}

}
