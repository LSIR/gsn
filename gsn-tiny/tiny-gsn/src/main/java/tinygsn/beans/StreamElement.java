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
* File: gsn-tiny/src/tinygsn/beans/StreamElement.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.beans;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tinygsn.utils.CaseInsensitiveComparator;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.TreeMap;


public final class StreamElement implements Serializable {

	private static final long serialVersionUID = 2000261462783698617L;

	private long timeStamp = -1;

	private String[] fieldNames;

	private Serializable[] fieldValues;

	private transient TreeMap < String , Integer > indexedFieldNames = null;
	
	private transient Byte[] fieldTypes;

	private transient long internalPrimayKey = -1;

	private static final String NULL_ENCODING = "NULL"; // null encoding for
	
	public StreamElement(StreamElement other) {
		this.fieldNames = new String[other.fieldNames.length];
		for (int i = 0; i < other.fieldNames.length; i++) {
			fieldNames[i] = other.fieldNames[i];
			fieldValues[i] = other.fieldValues[i];
			fieldTypes[i] = other.fieldTypes[i];
		}
		this.timeStamp = other.timeStamp;
		this.internalPrimayKey = other.internalPrimayKey;
	}

	public StreamElement(DataField[] outputStructure, final Serializable[] data) {
		this(outputStructure, data, System.currentTimeMillis());
	}

	public StreamElement(DataField[] outputStructure, final Serializable[] data,
			final long timeStamp) {
		this.fieldNames = new String[outputStructure.length];
		this.fieldTypes = new Byte[outputStructure.length];
		this.timeStamp = timeStamp;
		for (int i = 0; i < this.fieldNames.length; i++) {
			this.fieldNames[i] = outputStructure[i].getName().toLowerCase();
			this.fieldTypes[i] = outputStructure[i].getDataTypeID();
		}
		if (this.fieldNames.length != data.length)
			throw new IllegalArgumentException(
					"The length of dataFieldNames and the actual data provided in the constructor of StreamElement doesn't match.");
//		this.verifyTypesCompatibility(this.fieldTypes, data);
		this.fieldValues = data;
	}

	public StreamElement(final String[] dataFieldNames,
			final Byte[] dataFieldTypes, final Serializable[] data) {
		this(dataFieldNames, dataFieldTypes, data, System.currentTimeMillis());
	}

	public StreamElement(final String[] dataFieldNames,
			final Byte[] dataFieldTypes, final Serializable[] data,
			final long timeStamp) {
		if (dataFieldNames.length != dataFieldTypes.length)
			throw new IllegalArgumentException(
					"The length of dataFileNames and dataFileTypes provided in the constructor of StreamElement doesn't match.");
		if (dataFieldNames.length != data.length)
			throw new IllegalArgumentException(
					"The length of dataFileNames and the actual data provided in the constructor of StreamElement doesn't match.");
		this.timeStamp = timeStamp;
		this.fieldTypes = dataFieldTypes;
		this.fieldNames = dataFieldNames;
		this.fieldValues = data;
//		this.verifyTypesCompatibility(dataFieldTypes, data);
	}

	// public StreamElement(TreeMap<String, Serializable> output, DataField[]
	// fields) {
	// int nbFields = output.keySet().size();
	// if (output.containsKey("timed"))
	// nbFields--;
	// String fieldNames[] = new String[nbFields];
	// Byte fieldTypes[] = new Byte[nbFields];
	// Serializable fieldValues[] = new Serializable[nbFields];
	// TreeMap<String, Integer> indexedFieldNames = new TreeMap<String, Integer>(
	// new CaseInsensitiveComparator());
	// int idx = 0;
	//
	// long timestamp = System.currentTimeMillis();
	// for (String key : output.keySet()) {
	// Serializable value = output.get(key);
	//
	// if (key.equalsIgnoreCase("timed"))
	// timestamp = (Long) value;
	// else {
	// fieldNames[idx] = key;
	// fieldValues[idx] = value;
	// for (int i = 0; i < fields.length; i++) {
	// if (fields[i].getName().equalsIgnoreCase(key))
	// fieldTypes[idx] = fields[i].getDataTypeID();
	// }
	// indexedFieldNames.put(key, idx);
	// idx++;
	// }
	// }
	// this.fieldNames = fieldNames;
	// this.fieldTypes = fieldTypes;
	// this.fieldValues = fieldValues;
	// this.indexedFieldNames = indexedFieldNames;
	// this.timeStamp = timestamp;
	// }

	private void verifyTypesCompatibility(final Byte[] fieldTypes,
			final Serializable[] data) throws IllegalArgumentException {
		for (int i = 0; i < data.length; i++) {
			if (data[i] == null)
				continue;
			switch (fieldTypes[i]) {
			case DataTypes.TINYINT:
				if (!(data[i] instanceof Byte))
					throw new IllegalArgumentException(
							"The newly constructed Stream Element is not consistent. The "
									+ (i + 1) + "th field is defined as "
									+ DataTypes.TYPE_NAMES[fieldTypes[i]]
									+ " while the actual data in the field is of type : *"
									+ data[i].getClass().getCanonicalName() + "*");
				break;
			case DataTypes.SMALLINT:
				if (!(data[i] instanceof Short))
					throw new IllegalArgumentException(
							"The newly constructed Stream Element is not consistent. The "
									+ (i + 1) + "th field is defined as "
									+ DataTypes.TYPE_NAMES[fieldTypes[i]]
									+ " while the actual data in the field is of type : *"
									+ data[i].getClass().getCanonicalName() + "*");
				break;
			case DataTypes.BIGINT:
				if (!(data[i] instanceof Long)) {
					throw new IllegalArgumentException(
							"The newly constructed Stream Element is not consistant. The "
									+ (i + 1) + "th field is defined as "
									+ DataTypes.TYPE_NAMES[fieldTypes[i]]
									+ " while the actual data in the field is of type : *"
									+ data[i].getClass().getCanonicalName() + "*");
				}
				break;
			case DataTypes.CHAR:
			case DataTypes.VARCHAR:
				if (!(data[i] instanceof String)) {
					throw new IllegalArgumentException(
							"The newly constructed Stream Element is not consistant. The "
									+ (i + 1) + "th field is defined as "
									+ DataTypes.TYPE_NAMES[fieldTypes[i]]
									+ " while the actual data in the field is of type : *"
									+ data[i].getClass().getCanonicalName() + "*");
				}
				break;
			case DataTypes.INTEGER:
				if (!(data[i] instanceof Integer)) {
					throw new IllegalArgumentException(
							"The newly constructed Stream Element is not consistant. The "
									+ (i + 1) + "th field is defined as "
									+ DataTypes.TYPE_NAMES[fieldTypes[i]]
									+ " while the actual data in the field is of type : *"
									+ data[i].getClass().getCanonicalName() + "*");
				}
				break;
			case DataTypes.DOUBLE:
				if (!(data[i] instanceof Double || data[i] instanceof Float))
					throw new IllegalArgumentException(
							"The newly constructed Stream Element is not consistant. The "
									+ (i + 1) + "th field is defined as "
									+ DataTypes.TYPE_NAMES[fieldTypes[i]]
									+ " while the actual data in the field is of type : *"
									+ data[i].getClass().getCanonicalName() + "*");
				break;
			case DataTypes.BINARY:
				// if ( data[ i ] instanceof String ) data[ i ] = ( ( String )
				// data[ i ] ).getBytes( );
				if (!(data[i] instanceof byte[] || data[i] instanceof String))
					throw new IllegalArgumentException(
							"The newly constructed Stream Element is not consistant. The "
									+ (i + 1) + "th field is defined as "
									+ DataTypes.TYPE_NAMES[fieldTypes[i]]
									+ " while the actual data in the field is of type : *"
									+ data[i].getClass().getCanonicalName() + "*");
				break;
			}
		}
	}

    public static String toJSON(String vs_name, StreamElement[] s){

		GeoJsonField[] fields = new GeoJsonField[s[0].getFieldNames().length+1];
		fields[0] = new GeoJsonField();
		fields[0].setName("timestamp");
		fields[0].setType("time");
		fields[0].setUnit("ms");
		for(int i = 1; i < fields.length; i++){
			fields[i] = new GeoJsonField();
			fields[i].setName(s[0].getFieldNames()[i-1]);
			fields[i].setType(DataTypes.TYPE_NAMES[s[0].getFieldTypes()[i-1]]);
		}
		Serializable[][] values = new Serializable[s.length][fields.length];
		for(int i = 0; i < s.length; i++){
			values[i][0] = s[i].getTimeStamp();
			for(int j = 1; j < fields.length; j++){
				values[i][j] = s[i].getData()[j-1];
			}
		}
		GeoJsonProperties prop = new GeoJsonProperties();
		prop.setVs_name(vs_name);
		prop.setFields(fields);
		prop.setValues(values);
        GeoJsonFeature feature = new GeoJsonFeature();
        feature.setPage_size(1);
        feature.setTotal_size(1);
		feature.setType("Feature");
		feature.setProperties(prop);

		Gson gson = new Gson();
		return gson.toJson(feature);
    }

	/**
	 * Build stream elements from a JSON representation like this one:
	 * {"type":"Feature","properties":{"vs_name":"geo_oso3m","values":[[1464094800000,21,455.364922]],"fields":[{"name":"timestamp","type":"time","unit":"ms"},{"name":"station","type":"smallint","unit":null},{"name":"altitude","type":"float","unit":null}],"stats":{"start-datetime":1381953249010,"end-datetime":1464096133100},"geographical":"Lausanne, Switzerland","description":"OZ47 Sensor"},"geometry":{"type":"Point","coordinates":[6.565356337141691,46.5608445136986,689.7967]},"total_size":0,"page_size":0}
	 * Expecting the first value to be the timestamp
	 * @param s
	 * @return
	 */

	public static StreamElement[] fromJSON(String s){
        JsonParser parser = new JsonParser();
		JsonObject jn = parser.parse(s).getAsJsonObject().get("properties").getAsJsonObject();
		DataField[] df = new DataField[jn.get("fields").getAsJsonArray().size()-1];
		int i = 0;
		for(JsonElement f : jn.get("fields").getAsJsonArray()){
			if (f.getAsJsonObject().get("name").getAsString().equals("timestamp")) continue;
			df[i] = new DataField(f.getAsJsonObject().get("name").getAsString(),f.getAsJsonObject().get("type").getAsString());
			i++;
		}
		StreamElement[] ret = new StreamElement[jn.get("values").getAsJsonArray().size()];
		int k = 0;
		for(JsonElement v : jn.get("values").getAsJsonArray()){
			Serializable[] data = new Serializable[df.length];
			for(int j=1;j < v.getAsJsonArray().size();j++){
				switch(df[j].getDataTypeID()){
					case DataTypes.DOUBLE:
						data[j] = v.getAsJsonArray().get(j).getAsDouble();
						break;
					case DataTypes.FLOAT:
						data[j] = (float)v.getAsJsonArray().get(j).getAsDouble();
						break;
					case DataTypes.BIGINT:
						data[j] = v.getAsJsonArray().get(j).getAsLong();
						break;
					case DataTypes.TINYINT:
						data[j] = (byte)v.getAsJsonArray().get(j).getAsInt();
						break;
					case DataTypes.SMALLINT:
					case DataTypes.INTEGER:
						data[j] = v.getAsJsonArray().get(j).getAsInt();
						break;
					case DataTypes.CHAR:
					case DataTypes.VARCHAR:
						data[j] = v.getAsJsonArray().get(j).getAsString();
						break;
					case DataTypes.BINARY:
						data[j] = Base64.decode(v.getAsJsonArray().get(j).getAsString(), Base64.DEFAULT);
						break;
					default:
						//logger.error("The data type of the field cannot be parsed: " + df[j].toString());
				}
			}
			ret[k] = new StreamElement(df, data, v.getAsJsonArray().get(0).getAsLong());
		}
		return ret;
	}

	public String toString() {
		final StringBuffer output = new StringBuffer("timed = ");
		output.append(new Date(this.getTimeStamp())).append("\n");

		DecimalFormat df = new DecimalFormat("#.###");
		for (int i = 0; i < this.fieldNames.length; i++) {
			if (this.fieldTypes[i] == DataTypes.VARCHAR ||
				this.fieldTypes[i] == DataTypes.CHAR ||
				this.fieldTypes[i] == DataTypes.BINARY) {
				output.append("\t").append(this.fieldNames[i]).append(" = ")
						.append(this.fieldValues[i].toString() + "\n");
			} else {
				output.append("\t").append(this.fieldNames[i]).append(" = ")
						.append(df.format(this.fieldValues[i]) + "\n");
			}
		}
		return output.toString();
	}

	public final String[] getFieldNames() {
		return this.fieldNames;
	}

	public final Byte[] getFieldTypes() {
		return this.fieldTypes;
	}

	public final Serializable[] getData() {
		return this.fieldValues;
	}

	public final Serializable getData(final String fieldName) {
		// if ( indexedFieldNames == null ) {
		// indexedFieldNames = new TreeMap < String , Integer >( new
		// CaseInsensitiveComparator( ) );
		// for ( int i = 0 ; i < this.fieldNames.length ; i++ )
		// this.indexedFieldNames.put( fieldNames[ i ] , i );
		// // for (String k : this.indexedFieldNames.keySet())
		// // System.out.println("Key : "+k +
		// " VALUE = "+this.indexedFieldNames.get(k));
		// }
		// // System.out.print(fieldName+" AT INDEX : "+ this.indexedFieldNames.get(
		// fieldName ) );
		// // System.out.println(" HAS VALUE : "+this.fieldValues[
		// this.indexedFieldNames.get( fieldName ) ]);
		// Integer index = indexedFieldNames.get( fieldName );
		// if (index == null) {
		// logger.info("There is a request for field "+fieldName+" for StreamElement: "+this.toString()+". As the requested field doesn't exist, GSN returns Null to the callee.");
		// return null;
		// }
		int index = -1;
		for (int i = 0; i < this.fieldNames.length; i++) {
			if (fieldNames[i].equalsIgnoreCase(fieldName)) {
				index = i;
				break;
			}
		}

		if (index == -1)
			return null;
		else
			return this.fieldValues[index];
	}

	public void setData(int index, Serializable data) {
		this.fieldValues[index] = data;
	}
	
	/**
	 * Build the index for mapping field name to their positions in the array if it is not yet built
	 * This assumes that StreamElements cannot change their structure
	 */
	private void generateIndex(){
		if ( indexedFieldNames == null ) {
			indexedFieldNames = new TreeMap < String , Integer >( new CaseInsensitiveComparator( ) );
			for ( int i = 0 ; i < this.fieldNames.length ; i++ )
				this.indexedFieldNames.put( fieldNames[ i ] , i );
		}
	}
	
	public void setData(String fieldName, Serializable data) throws IllegalArgumentException {
		generateIndex();
		Integer index = indexedFieldNames.get( fieldName );
		if (index != null) {
			setData(index,data);		
		}
	}

	public long getTimeStamp() {
		return this.timeStamp;
	}

	public StringBuilder getFieldTypesInString() {
		final StringBuilder stringBuilder = new StringBuilder();
		for (final byte i : this.getFieldTypes())
			stringBuilder.append(DataTypes.TYPE_NAMES[i]).append(" , ");
		return stringBuilder;
	}

	public boolean isTimestampSet() {
		return this.timeStamp > 0;
	}

	public void setTimeStamp(long timeStamp) {
		if (this.timeStamp <= 0)
			timeStamp = 0;
		else
			this.timeStamp = timeStamp;
	}

	public long getInternalPrimayKey() {
		return internalPrimayKey;
	}

	public void setInternalPrimayKey(long internalPrimayKey) {
		this.internalPrimayKey = internalPrimayKey;
	}

}
