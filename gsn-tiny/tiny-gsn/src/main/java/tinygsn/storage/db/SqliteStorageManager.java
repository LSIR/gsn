/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/storage/db/SqliteStorageManager.java
 *
 * @author Do Ngoc Hoan
 */


package tinygsn.storage.db;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.DeliveryRequest;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.Subscription;
import tinygsn.beans.VSensorConfig;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.utils.Const;

/**
 *
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 *
 */
public class SqliteStorageManager extends StorageManager implements Serializable {


	private static final long serialVersionUID = 7774503312823392567L;
	private static final String TAG = "SqliteStorageManager";
	private SQLiteDatabase database;
	private static SQLiteDatabaseOpenHelper dbOpenHelper;

	public SqliteStorageManager() {
		super();
		this.isSQLite = true;
		dbOpenHelper = getInstance();
		File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
				                           + "/Android/data/ch.epfl.gsn.tiny");
		myFilesDir.mkdirs();
		database = dbOpenHelper.getWritableDatabase();
	}

	public static synchronized SQLiteDatabaseOpenHelper getInstance() {
		if (dbOpenHelper == null) {
			dbOpenHelper = new SQLiteDatabaseOpenHelper(StaticData.globalContext,
					Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/Android/data/ch.epfl.gsn.tiny/" + Const.DATABASE_NAME,
					null, Const.DATABASE_VERSION);
		}
		return dbOpenHelper;
	}

	@Override
	public void executeCreateTable(CharSequence tableName, DataField[] outputStructure, boolean uniq) {

		ArrayList<String> fields = new ArrayList<String>();

		for (DataField f : outputStructure) {
			fields.add(f.getName());
		}
		fields.add("timed");
		createTable(tableName, fields);
	}

	public void createTable(CharSequence vsName, ArrayList<String> fields) {
		String createQuery = "CREATE TABLE " + vsName
				                     + "(_id integer primary key autoincrement";

		for (String f : fields) {
			createQuery += ", " + f;
		}

		createQuery += ");";
		database.execSQL(createQuery);
	}

	public void executeInsertWifiFrequency(String macAdr) {
		ContentValues newCon = new ContentValues();
		newCon.put("frequency", 1);
		newCon.put("mac", macAdr);

		database.insert("WifiFrequency", null, newCon);

	}

	public void executeInsertSamples(int sample, int reason) {
		ContentValues newCon = new ContentValues();
		newCon.put("time", System.currentTimeMillis());
		newCon.put("sample", sample);
		newCon.put("reason", reason);

		database.insert("Samples", null, newCon);
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
			switch (se.getFieldTypes()[i]){
				case DataTypes.TIME:
				case DataTypes.BIGINT:
					newCon.put(se.getFieldNames()[i], (Long) se.getData(se.getFieldNames()[i]));
					break;
				case DataTypes.INTEGER:
					newCon.put(se.getFieldNames()[i], (Integer) se.getData(se.getFieldNames()[i]));
					break;
				case DataTypes.SMALLINT:
					newCon.put(se.getFieldNames()[i], (Short) se.getData(se.getFieldNames()[i]));
					break;
				case DataTypes.TINYINT:
					newCon.put(se.getFieldNames()[i], (Byte) se.getData(se.getFieldNames()[i]));
					break;
				case DataTypes.FLOAT:
					newCon.put(se.getFieldNames()[i], (Float) se.getData(se.getFieldNames()[i]));
					break;
				case DataTypes.DOUBLE:
					newCon.put(se.getFieldNames()[i], (Double) se.getData(se.getFieldNames()[i]));
					break;
				case DataTypes.VARCHAR:
				case DataTypes.CHAR:
				case DataTypes.BINARY:
					newCon.put(se.getFieldNames()[i], (String) se.getData(se.getFieldNames()[i]));
					break;
			}
		}

		newCon.put("timed", se.getTimeStamp());

		database.insert((String) tableName, null, newCon);
	}

	public void executeInsert(String tableName, ArrayList<String> fields,
	                          ArrayList<String> values) throws SQLException {
		ContentValues newCon = new ContentValues();
		for (int i = 0; i < fields.size(); i++) {
			newCon.put(fields.get(i), values.get(i));
		}
		database.insert((String) tableName, null, newCon);
	}

	public int[] getLatestState() {
		String query = "Select * from Samples order by time desc limit 1;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		if (cursor.moveToNext()) {
			return new int[]{cursor.getInt(cursor.getColumnIndex("sample")), cursor.getInt(cursor.getColumnIndex("reason"))};
		} else {
			return new int[]{0, 0};
		}
	}

	public ArrayList<StreamElement> executeQueryGetLatestValues(String tableName, String[] FIELD_NAMES, Byte[] FIELD_TYPES, int num) {
		return executeQueryGetLatestValues(tableName, FIELD_NAMES, FIELD_TYPES, num, 0);
	}

	/**
	 * Get num latest values
	 *
	 * @param tableName
	 * @param num
	 * @return
	 */
	public ArrayList<StreamElement> executeQueryGetLatestValues(String tableName, String[] FIELD_NAMES, Byte[] FIELD_TYPES, int num, long minTimestamp) {

		Serializable[] fieldValues;
		ArrayList<StreamElement> result = new ArrayList<StreamElement>();
		String query = "Select * from " + tableName + " where timed > " + minTimestamp + " order by _id desc limit ?";
		Cursor cursor = database.rawQuery(query, new String[]{num + ""});

		while (cursor.moveToNext()) {
			fieldValues = new Serializable[FIELD_NAMES.length];
			for (int i = 0; i < FIELD_NAMES.length; i++) {
				switch (FIELD_TYPES[i]){
					case DataTypes.VARCHAR:
					case DataTypes.CHAR:
					case DataTypes.BINARY:
						fieldValues[i] = cursor.getString(cursor.getColumnIndex(FIELD_NAMES[i].toLowerCase(Locale.ENGLISH)));
						break;
					case DataTypes.TIME:
					case DataTypes.BIGINT:
						fieldValues[i] = cursor.getLong(cursor.getColumnIndex(FIELD_NAMES[i].toLowerCase(Locale.ENGLISH)));
						break;
					case DataTypes.DOUBLE:
						fieldValues[i] = cursor.getDouble(cursor.getColumnIndex(FIELD_NAMES[i].toLowerCase(Locale.ENGLISH)));
						break;
					case DataTypes.FLOAT:
						fieldValues[i] = cursor.getFloat(cursor.getColumnIndex(FIELD_NAMES[i].toLowerCase(Locale.ENGLISH)));
						break;
					case DataTypes.INTEGER:
						fieldValues[i] = cursor.getInt(cursor.getColumnIndex(FIELD_NAMES[i].toLowerCase(Locale.ENGLISH)));
						break;
					case DataTypes.SMALLINT:
					case DataTypes.TINYINT:
						fieldValues[i] = cursor.getShort(cursor.getColumnIndex(FIELD_NAMES[i].toLowerCase(Locale.ENGLISH)));
				}
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));

			StreamElement se = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					                                    fieldValues, time);
			result.add(se);
		}
		cursor.close();
		return result;
	}

	public ArrayList<StreamElement> executeQueryGetLatestValues(String tableName, int num) throws SQLException {
		return executeQueryGetLatestValues(tableName, num, 0);
	}


	public ArrayList<StreamElement> executeQueryGetLatestValues(String tableName, int num, long minTimestamp) throws SQLException{

		Serializable[] fieldValues;
		DataField[] structure = tableToStructure(tableName);
		ArrayList<StreamElement> result = new ArrayList<StreamElement>();
		String query = "Select * from " + tableName + " where timed > " + minTimestamp + " order by _id desc limit ?";
		Cursor cursor = database.rawQuery(query, new String[]{num + ""});

		while (cursor.moveToNext()) {
			fieldValues = new Serializable[structure.length];
			for (int i = 0; i < structure.length; i++) {
				byte dtype = structure[i].getDataTypeID();
				String name = structure[i].getName().toLowerCase(Locale.ENGLISH);
				switch (dtype){
					case DataTypes.VARCHAR:
					case DataTypes.CHAR:
					case DataTypes.BINARY:
						fieldValues[i] = cursor.getString(cursor.getColumnIndex(name));
						break;
					case DataTypes.TIME:
					case DataTypes.BIGINT:
						fieldValues[i] = cursor.getLong(cursor.getColumnIndex(name));
						break;
					case DataTypes.DOUBLE:
						fieldValues[i] = cursor.getDouble(cursor.getColumnIndex(name));
						break;
					case DataTypes.FLOAT:
						fieldValues[i] = cursor.getFloat(cursor.getColumnIndex(name));
						break;
					case DataTypes.INTEGER:
						fieldValues[i] = cursor.getInt(cursor.getColumnIndex(name));
						break;
					case DataTypes.SMALLINT:
					case DataTypes.TINYINT:
						fieldValues[i] = cursor.getShort(cursor.getColumnIndex(name));
				}
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));

			StreamElement se = new StreamElement(structure, fieldValues, time);
			result.add(se);
		}
		cursor.close();
		return result;
	}

	public ArrayList<StreamElement> executeQueryGetRangeData(String vsName, long start, long end) throws SQLException{
		return executeQueryGetRangeData(vsName, start, end, 0);
	}

	public ArrayList<StreamElement> executeQueryGetRangeData(String vsName, long start, long end, long limit) throws SQLException{
		Serializable[] fieldValues;
		DataField[] structure = tableToStructure(vsName);
		ArrayList<StreamElement> result = new ArrayList<>();

		String query = "Select * from " + vsName + " where CAST(timed AS NUMERIC) >= ? AND CAST(timed AS NUMERIC) <= ? ORDER BY timed";

		if (limit > 0){
			query += " ASC limit " + limit;
		} else {
			query += " ASC";
		}

		Cursor cursor = database.rawQuery(query, new String[]{start + "", end + ""});

		while (cursor.moveToNext()) {
			fieldValues = new Serializable[structure.length];
			for (int i = 0; i < structure.length; i++) {
				byte dtype = structure[i].getDataTypeID();
				String name = structure[i].getName().toLowerCase(Locale.ENGLISH);
				switch (dtype){
					case DataTypes.VARCHAR:
					case DataTypes.CHAR:
					case DataTypes.BINARY:
						fieldValues[i] = cursor.getString(cursor.getColumnIndex(name));
						break;
					case DataTypes.TIME:
					case DataTypes.BIGINT:
						fieldValues[i] = cursor.getLong(cursor.getColumnIndex(name));
						break;
					case DataTypes.DOUBLE:
						fieldValues[i] = cursor.getDouble(cursor.getColumnIndex(name));
						break;
					case DataTypes.FLOAT:
						fieldValues[i] = cursor.getFloat(cursor.getColumnIndex(name));
						break;
					case DataTypes.INTEGER:
						fieldValues[i] = cursor.getInt(cursor.getColumnIndex(name));
						break;
					case DataTypes.SMALLINT:
					case DataTypes.TINYINT:
						fieldValues[i] = cursor.getShort(cursor.getColumnIndex(name));
				}
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));
			StreamElement se = new StreamElement(structure, fieldValues, time);
			result.add(se);
		}
		return result;
	}

	public ArrayList<StreamElement> executeQueryGetRangeData(String vsName,
	                                                         long start, long end, String[] FIELD_NAMES, Byte[] FIELD_TYPES) {
		Serializable[] fieldValues;
		String[] fieldNames;
		Byte[] fieldTypes;

		ArrayList<StreamElement> result = new ArrayList<StreamElement>();

		String query = "Select * from " + vsName
				               + " where CAST(timed AS NUMERIC) >= ? AND CAST(timed AS NUMERIC) <= ? ORDER BY timed ASC";

		Cursor cursor = database.rawQuery(query, new String[]{start + "",
				                                                     end + ""});

		while (cursor.moveToNext()) {
			fieldValues = new Serializable[FIELD_NAMES.length + 1];
			fieldNames = new String[FIELD_NAMES.length + 1];
			fieldTypes = new Byte[FIELD_NAMES.length + 1];
			for (int i = 0; i < FIELD_NAMES.length; i++) {
				fieldValues[i] = cursor
						                 .getDouble(cursor.getColumnIndex(FIELD_NAMES[i]));
				fieldNames[i] = FIELD_NAMES[i];
				fieldTypes[i] = FIELD_TYPES[i];
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));
			fieldNames[fieldNames.length - 1] = "userid";
			fieldTypes[fieldTypes.length - 1] = DataTypes.INTEGER;
			fieldValues[fieldValues.length - 1] = Integer.valueOf(Const.USER_ID);
			StreamElement se = new StreamElement(fieldNames, fieldTypes,
					                                    fieldValues, time);

			result.add(se);
		}
		return result;
	}

	public boolean updateWifiFrequency(String macAdr) {
		int frequency = getFrequencyByMac(macAdr);
		if (frequency != -1) {
			String query = "UPDATE WifiFrequency SET frequency = ? WHERE mac = ?;";
			Cursor cursor = database.rawQuery(query, new String[]{(frequency + 1) + "", macAdr});
			if (cursor.moveToNext())
				return true;
		} else
			executeInsertWifiFrequency(macAdr);
		return false;
	}

	@SuppressLint("UseSparseArrays")
	public Map<Long, Integer> getFrequencies() {
		Map<Long, Integer> freqs = new HashMap<Long, Integer>();
		String query = "Select * from WifiFrequency;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext()) {
			int frequency = cursor.getInt(cursor.getColumnIndex("frequency"));
			String mac = cursor.getString(cursor.getColumnIndex("mac"));
			mac = mac.replaceAll(":", "");
			freqs.put(Long.parseLong(mac, 16), frequency);
		}
		return freqs;
	}


	private int getFrequencyByMac(String macAdr) {
		String query = "Select * from WifiFrequency;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext()) {
			int frequency = cursor.getInt(cursor.getColumnIndex("frequency"));
			String mac = cursor.getString(cursor.getColumnIndex("mac"));
			if (mac.equals(macAdr))
				return frequency;
		}
		return -1;
	}

	public boolean update(String tableName, String vsName, String field,
	                      String value) {
		String query = "UPDATE " + tableName + " SET " + field + " = ? "
				               + " WHERE vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{value, vsName});

		if (cursor.moveToNext()) {
			return true;
		}
		return false;
	}

	public void deleteVS(String vsName) {
		String query = "DELETE from vsList where vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{vsName});
		if (cursor.moveToNext()) {
			return;
		}
	}

	public void deleteSS(String vsName) {
		String query = "DELETE from sourcesList where vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{vsName});
		if (cursor.moveToNext()) {
			return;
		}
	}

	public void deleteTable(String tableName) {
		String query = "DROP TABLE " + tableName;
		Cursor cursor = database.rawQuery(query, new String[]{});
		if (cursor.moveToNext()) {
			return;
		}
	}

	@Override
	public void initDatabaseAccess(Connection con) throws Exception {
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
			case Cursor.FIELD_TYPE_INTEGER:
				return DataTypes.INTEGER;
			case Cursor.FIELD_TYPE_STRING:
				return DataTypes.VARCHAR;
			case Cursor.FIELD_TYPE_FLOAT:
				return DataTypes.DOUBLE;
			case Cursor.FIELD_TYPE_BLOB:
				return DataTypes.BINARY;
			default:
				Log.e(TAG, "convertLocalTypeToGSN: The type can't be converted to GSN form : " + jdbcType);
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
	public StringBuilder getStatementUselessDataRemoval(String virtualSensorName,
	                                                    long storageSize) {
		return null;
	}

	@Override
	public StringBuilder getStatementRemoveUselessDataCountBased(
			                                                            String virtualSensorName, long storageSize) {
		return null;
	}

	public ArrayList<String> getListofVSName() {
		ArrayList<String> vsList = new ArrayList<String>();
		String query = "Select vsname from vsList;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext()) {
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			vsList.add(vsname);
		}
		return vsList;
	}


	@Override
	public ArrayList<AbstractVirtualSensor> getListofVS() {
		ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();
		String query = "Select * from vsList;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndex("_id"));
			int running = cursor.getInt(cursor.getColumnIndex("running"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			int vstype = cursor.getInt(cursor.getColumnIndex("vstype"));

			String processingClass = AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[vstype];

			AbstractVirtualSensor vs = StaticData.getProcessingClassByName(vsname);
			if (vs == null) {
				VSensorConfig vsc = new VSensorConfig(id, processingClass, vsname, getSourcesOfVS(vsname),
						                                     running == 1);

				try {
					vs = StaticData.getProcessingClassByVSConfig(vsc);
					StaticData.addConfig(id, vsc);
					StaticData.saveNameID(id, vsname);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (vs != null) vsList.add(vs);
		}
		return vsList;
	}

	;

	public AbstractVirtualSensor getVSByName(String vsName) {
		String query = "Select * from vsList where vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{vsName});

		if (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndex("_id"));
			int running = cursor.getInt(cursor.getColumnIndex("running"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			int vstype = cursor.getInt(cursor.getColumnIndex("vstype"));

			String processingClass = AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[vstype];

			AbstractVirtualSensor vs = StaticData.getProcessingClassByName(vsname);
			if (vs == null) {
				VSensorConfig vsc = new VSensorConfig(id, processingClass, vsname, getSourcesOfVS(vsName),
						                                     running == 1);
				try {
					vs = StaticData.getProcessingClassByVSConfig(vsc);
					StaticData.addConfig(id, vsc);
					StaticData.saveNameID(id, vsname);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			return vs;

		}
		return null;
	}

	public boolean vsExists(String vsName) {
		String query = "Select vsname from vsList where vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{vsName});
		if (cursor.moveToNext()) {
			return true;
		}
		return false;
	}

	public ArrayList<String> getVSfromSource(String name) {
		String query = "Select * from sourcesList where wrappername = ? order by vsname asc;";
		Cursor cursor = database.rawQuery(query, new String[]{name});
		ArrayList<String> r = new ArrayList<String>();
		while (cursor.moveToNext()) {
			r.add(cursor.getString(cursor.getColumnIndex("vsname")));
		}
		return r;
	}

	@Override
	public ArrayList<StreamSource> getSourcesOfVS(String name) {
		ArrayList<StreamSource> sources = new ArrayList<StreamSource>();
		String query = "Select * from sourcesList where vsname = ?;";
		// open();
		Cursor cursor = database.rawQuery(query, new String[]{name});

		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndex("_id"));
			int sswindow = cursor.getInt(cursor.getColumnIndex("sswindowsize"));
			int ssstep = cursor.getInt(cursor.getColumnIndex("ssstep"));
			boolean sstimebased = cursor.getString(cursor.getColumnIndex("sstimebased")).equals("true");
			int aggregator = cursor.getInt(cursor.getColumnIndex("ssaggregator"));
			String wrappername = cursor.getString(cursor.getColumnIndex("wrappername"));

			StreamSource ss = null;
			if (StaticData.sourceMap.containsKey(id)) {
				ss = StaticData.sourceMap.get(id);
			} else {
				ss = new StreamSource();
				ss.setId(id);
				StaticData.sourceMap.put(id, ss);
			}
			ss.setAggregator(aggregator);
			ss.setStep(ssstep);
			ss.setTimeBased(sstimebased);
			ss.setWindowSize(sswindow);
			try {
				ss.setWrapper(StaticData.getWrapperByName(wrappername));
			} catch (Exception e) {
				e.printStackTrace();
			}
			sources.add(ss);
		}
		return sources;
	}

	public boolean updateWrapperInfo(String name, int interval, int duration) {
		String query = "UPDATE wrapperList SET dcinterval = ?, dcduration = ? WHERE wrappername = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{interval + "", duration + "", name});
		if (cursor.moveToNext())
			return true;
		return false;
	}

	public int[] getWrapperInfo(String name) {
		String query = "Select * from wrapperList WHERE wrappername = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{name});

		while (cursor.moveToNext()) {
			int duration = cursor.getInt(cursor.getColumnIndex("dcduration"));
			int interval = cursor.getInt(cursor.getColumnIndex("dcinterval"));
			return new int[]{interval, duration};
		}
		return null;

	}

	public void setWrapperInfo(String name, int interval, int duration) {
		if (getWrapperInfo(name) == null) {
			ContentValues newCon = new ContentValues();
			newCon.put("wrappername", name);
			newCon.put("dcinterval", interval);
			newCon.put("dcduration", duration);
			database.insert("wrapperList", null, newCon);
		} else {
			updateWrapperInfo(name, interval, duration);
		}
	}

	public boolean updateSubscribeInfo(int id, String url, String vsname, int mode, long lastTime, long iterationTime, boolean active, String username, String password) {
		String query = "UPDATE subscribeSource SET url = ?, vsname = ?, mode = ?, lastTime = ?, iterationTime = ?, active = ?, username = ?, password = ?  WHERE _id = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{url, vsname, mode + "", lastTime + "", iterationTime + "", active ? "1" : "0", username, password,"" + id});
		if (cursor.moveToNext())
			return true;
		return false;
	}

	public Subscription getSubscribeInfo(int id) {
		String query = "Select * from subscribeSource WHERE _id = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{id + ""});

		while (cursor.moveToNext()) {
			String url = cursor.getString(cursor.getColumnIndex("url"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			int mode = cursor.getInt(cursor.getColumnIndex("mode"));
			long lastTime = cursor.getLong(cursor.getColumnIndex("lastTime"));
			long iterationTime = cursor.getLong(cursor.getColumnIndex("iterationTime"));
			boolean active = cursor.getString(cursor.getColumnIndex("active")).equals("1");
			String username = cursor.getString(cursor.getColumnIndex("username"));
			String password = cursor.getString(cursor.getColumnIndex("password"));
			Subscription su = new Subscription(url, mode, vsname, id, iterationTime);
			su.setActive(active);
			su.setLastTime(lastTime);
			su.setUsername(username);
			su.setPassword(password);
			return su;
		}
		return null;

	}

	public void setSubscribeInfo(int id, String url, String vsname, int mode, long lastTime, long iterationTime, boolean active, String username, String password) {
		if (id == -1 || getSubscribeInfo(id) == null) {
			ContentValues newCon = new ContentValues();
			newCon.put("url", url);
			newCon.put("vsname", vsname);
			newCon.put("mode", mode);
			newCon.put("lastTime", lastTime);
            newCon.put("iterationTime", iterationTime);
			newCon.put("active", active ? "1" : "0");
			newCon.put("username", username);
			newCon.put("password", password);
			database.insert("subscribeSource", null, newCon);
		} else {
			updateSubscribeInfo(id, url, vsname, mode, lastTime, iterationTime, active, username, password);
		}
	}

	public ArrayList<Subscription> getSubscribeList() {
		ArrayList<Subscription> r = new ArrayList<Subscription>();
		String query = "Select * from subscribeSource;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndex("_id"));
			String url = cursor.getString(cursor.getColumnIndex("url"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			int mode = cursor.getInt(cursor.getColumnIndex("mode"));
			long lastTime = cursor.getLong(cursor.getColumnIndex("lastTime"));
            long iterationTime = cursor.getLong(cursor.getColumnIndex("iterationTime"));
			boolean active = cursor.getString(cursor.getColumnIndex("active")).equals("1");
			String username = cursor.getString(cursor.getColumnIndex("username"));
			String password = cursor.getString(cursor.getColumnIndex("password"));
			Subscription su = new Subscription(url, mode, vsname, id, iterationTime);
			su.setActive(active);
			su.setLastTime(lastTime);
			su.setUsername(username);
			su.setPassword(password);
			r.add(su);
		}
		return r;

	}


	public boolean updatePublishInfo(int id, String url, String vsname, String clientId, String clientSecret, int mode, long lastTime, long iterationTime, boolean active) {
		String query = "UPDATE publishDestination SET url = ?, vsname = ?, clientId = ?, clientSecret = ?, mode = ?, lastTime = ?, iterationTime = ?, active = ?  WHERE _id = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{url, vsname, clientId, clientSecret, mode + "", lastTime + "", iterationTime + "", active ? "1" : "0", "" + id});
		if (cursor.moveToNext())
			return true;
		return false;
	}

	public DeliveryRequest getPublishInfo(int id) {
		String query = "Select * from publishDestination WHERE _id = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{id + ""});

		while (cursor.moveToNext()) {
			String url = cursor.getString(cursor.getColumnIndex("url"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			String clientId = cursor.getString(cursor.getColumnIndex("clientId"));
			String clientSecret = cursor.getString(cursor.getColumnIndex("clientSecret"));
			int mode = cursor.getInt(cursor.getColumnIndex("mode"));
			long lastTime = cursor.getLong(cursor.getColumnIndex("lastTime"));
			long iterationTime = cursor.getLong(cursor.getColumnIndex("iterationTime"));
			boolean active = cursor.getString(cursor.getColumnIndex("active")).equals("1");
			DeliveryRequest dr = new DeliveryRequest(url, clientId, clientSecret, mode, vsname, id, iterationTime);
			dr.setActive(active);
			dr.setLastTime(lastTime);
			return dr;
		}
		return null;
	}

	public void deletePublishInfo(int id) {
		String query = "DELETE FROM publishDestination WHERE _id = " + id + ";";
		database.execSQL(query);
	}

	public void setPublishInfo(int id, String url, String vsname, String clientId, String clientSecret, int mode, long lastTime, long iterationTime, boolean active) {
		if (id == -1 || getPublishInfo(id) == null) {
			ContentValues newCon = new ContentValues();
			newCon.put("url", url);
			newCon.put("vsname", vsname);
			newCon.put("clientId", clientId);
			newCon.put("clientSecret", clientSecret);
			newCon.put("mode", mode);
			newCon.put("lastTime", lastTime);
			newCon.put("iterationTime", iterationTime);
			newCon.put("active", active ? "1" : "0");
			database.insert("publishDestination", null, newCon);
		} else {
			updatePublishInfo(id, url, vsname, clientId, clientSecret, mode, lastTime, iterationTime, active);
		}
	}

	public ArrayList<DeliveryRequest> getPublishList() {
		ArrayList<DeliveryRequest> r = new ArrayList<DeliveryRequest>();
		String query = "Select * from publishDestination;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext()) {
			int id = cursor.getInt(cursor.getColumnIndex("_id"));
			String url = cursor.getString(cursor.getColumnIndex("url"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			String clientId = cursor.getString(cursor.getColumnIndex("clientId"));
			String clientSecret = cursor.getString(cursor.getColumnIndex("clientSecret"));
			int mode = cursor.getInt(cursor.getColumnIndex("mode"));
			long lastTime = cursor.getLong(cursor.getColumnIndex("lastTime"));
			long iterationTime = cursor.getLong(cursor.getColumnIndex("iterationTime"));
			boolean active = cursor.getString(cursor.getColumnIndex("active")).equals("1");
			DeliveryRequest dr = new DeliveryRequest(url, clientId, clientSecret, mode, vsname, id, iterationTime);
			dr.setActive(active);
			dr.setLastTime(lastTime);
			r.add(dr);
		}
		return r;
	}

	public DataField[] tableToStructure(CharSequence tableName) throws SQLException {
		StringBuilder sb = new StringBuilder("select * from ").append(tableName).append(" limit 1");
		Cursor cursor = database.rawQuery(sb.toString(), new String[]{});
		boolean c = cursor.moveToFirst();
		ArrayList<DataField> toReturnArr = new ArrayList<DataField>();
		for (int i = 0; i < cursor.getColumnCount(); i++) {
			String colName = cursor.getColumnName(i);
			if (colName.equalsIgnoreCase("_id") || colName.equalsIgnoreCase("timed"))
				continue;
			int colType = Cursor.FIELD_TYPE_STRING;
			if (c) { //can only get type from data
				colType = cursor.getType(i);
			}
			byte colTypeInGSN = convertLocalTypeToGSN(colType);
			toReturnArr.add(new DataField(colName, colTypeInGSN));
		}
		return toReturnArr.toArray(new DataField[]{});

	}

	public HashMap<String, String> getSetting(String keyPrefix) {
		String query = "Select * from settings WHERE key LIKE ?;";
		Cursor cursor = database.rawQuery(query, new String[]{keyPrefix + "%"});
		HashMap<String, String> res = new HashMap<String, String>();
		while (cursor.moveToNext()) {
			String v = cursor.getString(cursor.getColumnIndex("value"));
			String k = cursor.getString(cursor.getColumnIndex("key"));
			res.put(k, v);
		}
		return res;
	}

	public void setSetting(String key, String value) {
		ContentValues newCon = new ContentValues();
		newCon.put("key", key);
		newCon.put("value", value);
		if (getSetting(key).size() != 0) {
			database.update("settings", newCon, "key = ?", new String[]{key});
		} else {
			database.insert("settings", null, newCon);
		}
	}

	public void deleteSetting(String key) {
		String query = "DELETE FROM settings WHERE key LIKE '" + key + "%';";
		database.execSQL(query);
	}

}
