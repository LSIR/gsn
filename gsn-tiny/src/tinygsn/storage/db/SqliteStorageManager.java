/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/storage/db/SqliteStorageManager.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.storage.db;

import java.io.File;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.StreamSource;
import tinygsn.beans.VSensorConfig;
import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.storage.StorageManager;
import tinygsn.utils.Const;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

/**
 * 
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 * 
 */
public class SqliteStorageManager extends StorageManager implements Serializable{


	private static final long serialVersionUID = 7774503312823392567L;
	private SQLiteDatabase database;
	private static SQLiteDatabaseOpenHelper dbOpenHelper;
	
	public SqliteStorageManager(Context context) {
		super();
		this.isSQLite = true;
		dbOpenHelper = getInstance(context);
		File myFilesDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/tinygsn" );
		myFilesDir.mkdirs();
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

	public void executeInsertWifiFrequency(String macAdr)
	{
		ContentValues newCon = new ContentValues();
		newCon.put("frequency", 1);
		newCon.put("mac", macAdr);
		
		database.insert("WifiFrequency", null, newCon);

	}
	
	public void executeInsertSamples(int sample,int reason)
	{
		ContentValues newCon = new ContentValues();
		newCon.put("time", System.currentTimeMillis());
		newCon.put("sample", sample);
		newCon.put("reason", reason);
		
		database.insert("Samples", null, newCon);
	}
	
	public void executeInsertSamplingRate(String vsName, int samplingRate)
	{
		ContentValues newCon = new ContentValues();
		newCon.put("time", System.currentTimeMillis());
		newCon.put("samplingrate", samplingRate);
		newCon.put("vsname", vsName);
		
		database.insert("SAMPLIG_RATE", null, newCon);
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
	
	public int[] getLatestState(){
		String query = "Select * from Samples order by time desc limit 1;";
		Cursor cursor = database.rawQuery(query, new String[] {});
		if (cursor.moveToNext()){
			return new int[]{cursor.getInt(cursor.getColumnIndex("sample")),cursor.getInt(cursor.getColumnIndex("reason"))};
		}else{
			return new int[]{0,0};
		}
	}

	public ArrayList<StreamElement> executeQueryGetLatestValues(String tableName,
			String[] FIELD_NAMES, Byte[] FIELD_TYPES, int num) {
		return executeQueryGetLatestValues(tableName,FIELD_NAMES,FIELD_TYPES,num,0);
	}
	
	/**
	 * Get num latest values
	 * 
	 * @param tabletName
	 * @param num
	 * @return
	 */
	public ArrayList<StreamElement> executeQueryGetLatestValues(String tableName,
			String[] FIELD_NAMES, Byte[] FIELD_TYPES, int num, long minTimestamp) {
		
		Serializable[] fieldValues;
		ArrayList<StreamElement> result = new ArrayList<StreamElement>();
		String query = "Select * from " + tableName + " where timed > "+minTimestamp+" order by _id desc limit ?";
		Cursor cursor = database.rawQuery(query, new String[] { num + "" });

		while (cursor.moveToNext()) {
			fieldValues = new Serializable[FIELD_NAMES.length];
			for (int i = 0; i < FIELD_NAMES.length; i++) {
				fieldValues[i] = cursor.getDouble(cursor.getColumnIndex(FIELD_NAMES[i].toLowerCase()));
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));

			StreamElement se = new StreamElement(FIELD_NAMES, FIELD_TYPES,
					fieldValues, time);
			result.add(se);
		}
		cursor.close();
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

		Cursor cursor = database.rawQuery(query, new String[] { start + "",
				end + "" });

		while (cursor.moveToNext()) {
			fieldValues = new Serializable[FIELD_NAMES.length+1];
			fieldNames = new String[FIELD_NAMES.length+1];
			fieldTypes = new Byte[FIELD_NAMES.length+1];
			for (int i = 0; i < FIELD_NAMES.length; i++) {
				fieldValues[i] = cursor
						.getDouble(cursor.getColumnIndex(FIELD_NAMES[i]));
				fieldNames[i] = FIELD_NAMES[i];
				fieldTypes[i] = FIELD_TYPES[i];
			}
			long time = cursor.getLong(cursor.getColumnIndex("timed"));
			fieldNames[fieldNames.length-1] = "userid";
			fieldTypes[fieldTypes.length-1] = DataTypes.INTEGER;
			fieldValues[fieldValues.length-1] = Integer.valueOf(Const.USER_ID);
			StreamElement se = new StreamElement(fieldNames, fieldTypes,
					fieldValues, time);

			result.add(se);
		}
		return result;
	}
	
	public Map<String, Integer> getSamplingRates()
	{
		Map<String, Integer> samplingRates = new HashMap<String, Integer>();
		String query = "Select * from SAMPLIG_RATE;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext())
		{
			int samplingRate = cursor.getInt(cursor.getColumnIndex("samplingrate"));
			String vsName = cursor.getString(cursor.getColumnIndex("vsname"));
			samplingRates.put(vsName, samplingRate);
		}
		return samplingRates;
	}
	
	public int getSamplingRateByName(String vsname)
	{
		String query = "Select * from SAMPLIG_RATE WHERE vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[]{vsname});
		
		while (cursor.moveToNext()) {
			int samplingRate = cursor.getInt(cursor.getColumnIndex("samplingrate"));
			String vsName = cursor.getString(cursor.getColumnIndex("vsname"));	
			if(vsName.equals(vsname))
				return samplingRate;			
		}
		return -1;

	}
		
	public boolean updateWifiFrequency(String macAdr)
	{
		int frequency = getFrequencyByMac(macAdr);
		if(frequency != -1)
		{
			String query = "UPDATE WifiFrequency SET frequency = ? WHERE mac = ?;";
			Cursor cursor = database.rawQuery(query, new String[] {(frequency+1)+"", macAdr});
			if(cursor.moveToNext())
				return true;
		}
		else 
			executeInsertWifiFrequency(macAdr);
		return false;
	}
	
	@SuppressLint("UseSparseArrays")
	public Map<Long, Integer> getFrequencies()
	{
		Map<Long, Integer> freqs = new HashMap<Long, Integer>();
		String query = "Select * from WifiFrequency;";
		Cursor cursor = database.rawQuery(query, new String[]{});
		while (cursor.moveToNext())
		{
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
		while (cursor.moveToNext())
		{
			int frequency = cursor.getInt(cursor.getColumnIndex("frequency"));
			String mac = cursor.getString(cursor.getColumnIndex("mac"));
			if(mac.equals(macAdr))
				return frequency;
		}
		return -1;
	}

	public boolean updateSamplingRate(String feild, int i) //for updating sampling rates from the activity for the schedular
	{
		String query = "UPDATE SAMPLIG_RATE SET samplingrate = ? WHERE vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[] {i+"", feild});
		if(cursor.moveToNext())
			return true;
		return false;
	}
	
	
	public boolean update(String tableName, String vsName, String field,
			String value) {
		String query = "UPDATE " + tableName + " SET " + field + " = ? "
				+ " WHERE vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[] { value, vsName });
		
		if (cursor.moveToNext()) {
			return true;
		}
		return false;
	}

	public void deleteVS(String vsName) {
		String query = "DELETE from vsList where vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[] { vsName });
		if (cursor.moveToNext()) {
			return;
		}
	}

	public void deleteTable(String tableName) {
		String query = "DROP TABLE " + tableName;
		Cursor cursor = database.rawQuery(query, new String[] {});
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
	public StringBuilder getStatementUselessDataRemoval(String virtualSensorName,
			long storageSize) {
		return null;
	}

	@Override
	public StringBuilder getStatementRemoveUselessDataCountBased(
			String virtualSensorName, long storageSize) {
		return null;
	}

	@Override
	public ArrayList<AbstractVirtualSensor> getListofVS() {
		ArrayList<AbstractVirtualSensor> vsList = new ArrayList<AbstractVirtualSensor>();
		String query = "Select * from vsList;";
		Cursor cursor = database.rawQuery(query, new String[] {});
		while (cursor.moveToNext()) {
			int id  = cursor.getInt(cursor.getColumnIndex("_id"));
			int running = cursor.getInt(cursor.getColumnIndex("running"));
			String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
			int vstype = cursor.getInt(cursor.getColumnIndex("vstype"));
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
	
			String processingClass = AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[vstype];
			
			
			VSensorConfig vsc = new VSensorConfig(id ,processingClass, vsname, getSourcesOfVS(vsname),
					 running == 1, notify_field, notify_condition,
					notify_value, notify_action, notify_contact, save_to_db);
			
			AbstractVirtualSensor vs = null;
			try {
				vs = StaticData.getProcessingClassByVSConfig(vsc);
				vsList.add(vs);
				StaticData.addConfig(id, vsc);
				StaticData.saveNameID(id, vsname);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return vsList;
	};
	
		public AbstractVirtualSensor getVSByName(String vsName)
		{
			String query = "Select * from vsList where vsname = ?;";
			Cursor cursor = database.rawQuery(query, new String[] { vsName });
			
			while (cursor.moveToNext()) {
				int id  = cursor.getInt(cursor.getColumnIndex("_id"));
				int running = cursor.getInt(cursor.getColumnIndex("running"));
				String vsname = cursor.getString(cursor.getColumnIndex("vsname"));
				int vstype = cursor.getInt(cursor.getColumnIndex("vstype"));
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
		
				String processingClass = AbstractVirtualSensor.VIRTUAL_SENSOR_CLASSES[vstype];
				
				
				VSensorConfig vsc = new VSensorConfig(id ,processingClass, vsname, getSourcesOfVS(vsName),
						 running == 1, notify_field, notify_condition,
						notify_value, notify_action, notify_contact, save_to_db);
				
				AbstractVirtualSensor vs = null;
				try {
					vs = StaticData.getProcessingClassByVSConfig(vsc);
					StaticData.addConfig(id, vsc);
					StaticData.saveNameID(id, vsname);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				return vs;
				
			}
			return null;
		}

	public boolean vsExists(String vsName) {
		String query = "Select vsname from vsList where vsname = ?;";
		Cursor cursor = database.rawQuery(query, new String[] { vsName });
		while (cursor.moveToNext()) {
			return true;
		}
		return false;
	}

	@Override
	public ArrayList<StreamSource> getSourcesOfVS(String name) {
		ArrayList<StreamSource> sources = new ArrayList<StreamSource>();
		String query = "Select * from sourcesList where vsname = ?;";
		// open();
		Cursor cursor = database.rawQuery(query, new String[] { name });
		
		while (cursor.moveToNext()) {
			int id  = cursor.getInt(cursor.getColumnIndex("_id"));
			int sswindow = cursor.getInt(cursor.getColumnIndex("sswindowsize"));
			int ssstep = cursor.getInt(cursor.getColumnIndex("ssstep"));
			boolean sstimebased = cursor.getShort(cursor.getColumnIndex("sstimebased"))==1;
			int sssamplingrate = cursor.getInt(cursor.getColumnIndex("sssamplingrate"));
			int aggregator = cursor.getInt(cursor.getColumnIndex("ssaggregator"));
			String wrappername = cursor.getString(cursor.getColumnIndex("wrappername"));
	
			StreamSource ss = null;
			if(StaticData.sourceMap.containsKey(id)){
				ss = StaticData.sourceMap.get(id);
			}else{
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
			}
			ss.getWrapper().setSamplingRate(sssamplingrate);
			sources.add(ss);
		}
		return sources;
	}

}
