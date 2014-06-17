package tinygsn.storage.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StreamElement;
import tinygsn.beans.VSensorConfig;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.vsensor.VirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.utils.Const;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * 
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 * 
 */
public class SqliteStorageManager extends StorageManager {

	// private static final String DB_NAME = "tinygsn11.db";
	private SQLiteDatabase database;
	private static SQLiteDatabaseOpenHelper dbOpenHelper;

	private static final String TAG = "SqliteStorageManager";

	public SqliteStorageManager(Context context) {
		super();
		this.isSQLite = true;
		dbOpenHelper = getInstance(context);
		database = dbOpenHelper.getWritableDatabase();
	}

	public static synchronized SQLiteDatabaseOpenHelper getInstance(
			Context context) {
		if (dbOpenHelper == null) {
			dbOpenHelper = new SQLiteDatabaseOpenHelper(context, Const.DATABASE_NAME,
					null, Const.DATABASE_VERSION);
		}
		return dbOpenHelper;
	}

	// public void open() throws SQLException {
	// // open database in reading/writing mode
	// database = dbOpenHelper.getWritableDatabase();
	// }
	//
	// public void close() {
	// if (database != null)
	// database.close();
	// }

	public void createTable(String vsName, DataField[] outputStructure) {
		ArrayList<String> fields = new ArrayList<String>();

		for (DataField f : outputStructure) {
			fields.add(f.getName());
		}
		fields.add("timed");
		createTable(vsName, fields);
	}

	public void createTable(String vsName, ArrayList<String> fields) {
		// open();
		String createQuery = "CREATE TABLE " + vsName
				+ "(_id integer primary key autoincrement";

		for (String f : fields) {
			createQuery += ", " + f;
		}

		createQuery += ");";
		database.execSQL(createQuery);
		// close();
	}

	/**
	 * As PreparedStatement on Android can't apply for Query (only for Insert,
	 * Update) => Therefore, we have to override the function.
	 */
	// synchronized
	@Override
	public void executeInsert(CharSequence tableName, DataField[] fields,
			StreamElement se) throws SQLException {
		ContentValues newCon = new ContentValues();
		for (int i = 0; i < se.getFieldNames().length; i++) {
			newCon.put(se.getFieldNames()[i], se.getData(se.getFieldNames()[i]) + "");
		}

		newCon.put("timed", se.getTimeStamp());

		// open();
		database.insert((String) tableName, null, newCon);
		// close();
		// Log.v(TAG, "Inserted se=" + se.toString());
	}

	public void executeInsert(String tableName, ArrayList<String> fields,
			ArrayList<String> values) throws SQLException {
		ContentValues newCon = new ContentValues();
		for (int i = 0; i < fields.size(); i++) {
			newCon.put(fields.get(i), values.get(i));
		}

		// open();
		database.insert((String) tableName, null, newCon);
		// close();
	}

	public ArrayList<StreamElement> executeQuery() {

		String[] FIELD_NAMES = new String[] { "latitude", "longitude" };
		Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE, DataTypes.DOUBLE };

		ArrayList<StreamElement> result = new ArrayList<StreamElement>();

		// try {
		// open();
		// }
		// catch (SQLException e) {
		// e.printStackTrace();
		// }
		Cursor cursor = database.query("gps", new String[] { "_id", "longitude",
				"latitude" }, null, null, null, null, null);
		while (cursor.moveToNext()) {
			double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
			double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));

			StreamElement se = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					new Serializable[] { latitude, longitude });

			result.add(se);
		}
		// close();

		return result;
	}

	/**
	 * Get num latest values
	 * 
	 * @param tabletName
	 * @param num
	 * @return
	 */
	public ArrayList<StreamElement> executeQueryGetLatestValues(String tableName,
			String[] FIELD_NAMES, Byte[] FIELD_TYPES, int num) {

		// String[] FIELD_NAMES = (String[]) fieldList.toArray();
		// Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE, DataTypes.DOUBLE };
		Serializable[] fieldValues;

		ArrayList<StreamElement> result = new ArrayList<StreamElement>();

		// try {
		// open();
		// }
		// catch (SQLException e) {
		// e.printStackTrace();
		// }
		String query = "Select max(_id) as maxid from " + tableName;
		Cursor cursor = database.rawQuery(query, new String[] {});
		long max = 0;
		if (cursor.moveToNext()) {
			max = cursor.getLong(cursor.getColumnIndex("maxid"));
		}

		query = "Select * from " + tableName + " where _id > ?";

		cursor = database.rawQuery(query, new String[] { max - num + "" });

		while (cursor.moveToNext()) {
			// double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
			// double longitude =
			// cursor.getDouble(cursor.getColumnIndex("longitude"));
			fieldValues = new Serializable[FIELD_NAMES.length];

			for (int i = 0; i < FIELD_NAMES.length; i++) {
				fieldValues[i] = cursor
						.getDouble(cursor.getColumnIndex(FIELD_NAMES[i]));
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));

			StreamElement se = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					fieldValues, time);
			// Log.v(TAG, se.toString());

			result.add(se);
		}
		// close();

		return result;
	}

	public ArrayList<StreamElement> executeQueryGetValues(String tableName,
			long start, long end) {
		String[] FIELD_NAMES = new String[] { "latitude", "longitude" };
		Byte[] FIELD_TYPES = new Byte[] { DataTypes.DOUBLE, DataTypes.DOUBLE };

		ArrayList<StreamElement> result = new ArrayList<StreamElement>();

		// try {
		// open();
		// }
		// catch (SQLException e) {
		// e.printStackTrace();
		// }
		String query;

		query = "Select * from " + tableName + " where timed > ? and timed < ?";

		Cursor cursor = database.rawQuery(query, new String[] { start + "",
				end + "" });

		while (cursor.moveToNext()) {
			double latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
			double longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
			long time = cursor.getLong(cursor.getColumnIndex("timed"));

			StreamElement se = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					new Serializable[] { latitude, longitude }, time);

			result.add(se);
		}
		// close();

		return result;
	}

	public ArrayList<StreamElement> executeQueryGetRangeData(String vsName,
			long start, long end, String[] FIELD_NAMES, Byte[] FIELD_TYPES) {
		Serializable[] fieldValues;

		// try {
		// open();
		// }
		// catch (SQLException e) {
		// e.printStackTrace();
		// }

		ArrayList<StreamElement> result = new ArrayList<StreamElement>();

		String query = "Select * from " + vsName
				+ " where CAST(timed AS NUMERIC) >= ? AND CAST(timed AS NUMERIC) <= ?";

		Cursor cursor = database.rawQuery(query, new String[] { start + "",
				end + "" });

		while (cursor.moveToNext()) {
			fieldValues = new Serializable[FIELD_NAMES.length];

			for (int i = 0; i < FIELD_NAMES.length; i++) {
				fieldValues[i] = cursor
						.getDouble(cursor.getColumnIndex(FIELD_NAMES[i]));
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));

			StreamElement se = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					fieldValues, time);
			// Log.v(TAG, se.toString());

			result.add(se);
		}
		// close();

		return result;
	}

	@Override
	public ArrayList<VirtualSensor> getListofVS() {
		ArrayList<VirtualSensor> vsList = new ArrayList<VirtualSensor>();
		String query = "Select * from vsList;";
		Cursor cursor = database.rawQuery(query, new String[] {});
		while (cursor.moveToNext()) {
			int running = cursor.getInt(cursor.getColumnIndex("running"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			double vstype = cursor.getDouble(cursor.getColumnIndex("vstype"));
			double sswindow = cursor.getDouble(cursor.getColumnIndex("sswindowsize"));
			double ssstep = cursor.getDouble(cursor.getColumnIndex("ssstep"));
			double sssamplingrate = cursor.getDouble(cursor
					.getColumnIndex("sssamplingrate"));
			int aggregator = cursor.getInt(cursor.getColumnIndex("ssaggregator"));
			String wrappername = cursor.getString(cursor
					.getColumnIndex("wrappername"));
			// "notify_field",
			// "notify_condition", "notify_value", "notify_action",
			// "notify_contact", "save_to_db"
			String notify_field = cursor.getString(cursor
					.getColumnIndex("notify_field"));
			String notify_condition = cursor.getString(cursor
					.getColumnIndex("notify_condition"));
			Double notify_value = cursor.getDouble(cursor
					.getColumnIndex("notify_value"));
			String notify_action = cursor.getString(cursor
					.getColumnIndex("notify_action"));
			String notify_contact = cursor.getString(cursor
					.getColumnIndex("notify_contact"));
			boolean save_to_db = cursor
					.getString(cursor.getColumnIndex("save_to_db")).equals("true");

			// Log.v(TAG, "save_to_db is " + save_to_db);

			String processingClass;
			if (vstype == 1)
				processingClass = AbstractVirtualSensor.PROCESSING_CLASS_BRIDGE;
			else
				processingClass = AbstractVirtualSensor.PROCESSING_CLASS_NOTIFICATION;

			VSensorConfig vs = new VSensorConfig(processingClass, vsname,
					wrappername, (int) sssamplingrate, (int) sswindow, (int) ssstep,
					aggregator, running == 1, notify_field, notify_condition,
					notify_value, notify_action, notify_contact, save_to_db);

			vsList.add(new VirtualSensor(vs));
		}

		return vsList;
	};

	public boolean vsExists(String vsName) {
		String query = "Select vsname from vsList where vsname = ?;";
		// open();
		Cursor cursor = database.rawQuery(query, new String[] { vsName });
		while (cursor.moveToNext()) {
			// if (cursor.getString(cursor.getColumnIndex("vsname")).equals(vsName))
			return true;
		}
		// close();
		return false;
	}

	public boolean update(String tableName, String vsName, String field,
			String value) {
		String query = "UPDATE " + tableName + " SET " + field + " = ? "
				+ " WHERE vsname = ?;";
		// open();
		Cursor cursor = database.rawQuery(query, new String[] { value, vsName });
		Log.v(TAG, cursor.toString());

		if (cursor.moveToNext()) {
			return true;
		}
		// close();
		return false;
	}

	public void deleteVS(String vsName) {
		String query = "DELETE from vsList where vsname = ?;";
		// open();
		Cursor cursor = database.rawQuery(query, new String[] { vsName });
		if (cursor.moveToNext()) {
			return;
		}
		// close();
	}

	public void deleteTable(String tableName) {
		String query = "DROP TABLE " + tableName;
		// open();
		Cursor cursor = database.rawQuery(query, new String[] {});
		if (cursor.moveToNext()) {
			return;
		}
		// close();
	}

	// public ArrayList<String> getFieldList(String vsName) {
	// ArrayList<String> fList = new ArrayList<String>();
	// String query = "Select * from vsList;";
	//
	// return fList;
	// }

	@Override
	public void initDatabaseAccess(Connection con) throws Exception {
		// Statement stmt = con.createStatement();
		// stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");
		// stmt.execute("CREATE ALIAS IF NOT EXISTS NOW_MILLIS FOR \"java.lang.System.currentTimeMillis\";");
		super.initDatabaseAccess(con);
	}

	@Override
	public String getJDBCPrefix() {
		return "";
	}

	@Override
	public String convertGSNTypeToLocalType(DataField gsnType) {
		String convertedType = null;
		switch (gsnType.getDataTypeID()) {
		case DataTypes.CHAR:
		case DataTypes.VARCHAR:
			// Because the parameter for the varchar is not
			// optional.
			if (gsnType.getType().trim().equalsIgnoreCase("string"))
				convertedType = "TEXT";
			else
				convertedType = gsnType.getType();
			break;
		default:
			convertedType = DataTypes.TYPE_NAMES[gsnType.getDataTypeID()];
			break;
		}
		return convertedType;
	}

	@Override
	public byte convertLocalTypeToGSN(int jdbcType, int precision) {
		switch (jdbcType) {
		case Types.BIGINT:
			return DataTypes.BIGINT;
		case Types.INTEGER:
			return DataTypes.INTEGER;
		case Types.SMALLINT:
			return DataTypes.SMALLINT;
		case Types.TINYINT:
			return DataTypes.TINYINT;
		case Types.VARCHAR:
			return DataTypes.VARCHAR;
		case Types.CHAR:
			return DataTypes.CHAR;
		case Types.DOUBLE:
		case Types.DECIMAL: // This is needed for doing aggregates in datadownload
												// servlet.
			return DataTypes.DOUBLE;
		case Types.BINARY:
		case Types.BLOB:
		case Types.VARBINARY:
		case Types.LONGVARBINARY:
			return DataTypes.BINARY;
		default:
			// logger.error("The type can't be converted to GSN form : " + jdbcType);
			break;
		}
		return -100;
	}

	@Override
	public String getStatementDropIndex() {
		return "DROP INDEX #NAME";
	}

	@Override
	public String getStatementDropView() {
		return "DROP VIEW #NAME IF EXISTS";
	}

	@Override
	public int getTableNotExistsErrNo() {
		return 42102;
	}

	@Override
	public String addLimit(String query, int limit, int offset) {
		return query + " LIMIT " + limit + " OFFSET " + offset;
	}

	@Override
	public String getStatementDifferenceTimeInMillis() {
		return "call NOW_MILLIS()";
	}

	@Override
	public StringBuilder getStatementDropTable(CharSequence tableName,
			Connection conn) throws SQLException {
		StringBuilder sb = new StringBuilder("Drop table if exists ");
		sb.append(tableName);
		return sb;
	}

	@Override
	public StringBuilder getStatementCreateTable(String tableName,
			DataField[] structure) {
		StringBuilder result = new StringBuilder("CREATE TABLE ").append(tableName);
		return result;
	}

	@Override
	public void shutdown() throws SQLException {
		getConnection().createStatement().execute("SHUTDOWN");
		// logger.warn("Closing the database server (for HSqlDB) [done].");
		// logger.warn("Closing the connection pool [done].");
		super.shutdown();
	}

	@Override
	public StringBuilder getStatementUselessDataRemoval(String virtualSensorName,
			long storageSize) {
		return null;
	}

	@Override
	public StringBuilder getStatementRemoveUselessDataCountBased(
			String virtualSensorName, long storageSize) {
		return null;
	}

}
