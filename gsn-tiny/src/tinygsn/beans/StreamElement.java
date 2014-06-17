package tinygsn.beans;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Date;
import android.util.Log;

public final class StreamElement implements Serializable {

	private static final long serialVersionUID = 2000261462783698617L;

	private long timeStamp = -1;

	private String[] fieldNames;

	private Serializable[] fieldValues;

	private transient Byte[] fieldTypes;

	private transient long internalPrimayKey = -1;

	private static final String NULL_ENCODING = "NULL"; // null encoding for
																											// transmission over
																											// xml-rpc
	private static final String TAG = "StreamElement";
	
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
					"The length of dataFileNames and the actual data provided in the constructor of StreamElement doesn't match.");
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

	public String toString() {
		final StringBuffer output = new StringBuffer("timed = ");
		output.append(new Date(this.getTimeStamp())).append("\n");

		DecimalFormat df = new DecimalFormat("#.###");
		for (int i = 0; i < this.fieldNames.length; i++)
			output.append("\t").append(this.fieldNames[i]).append(" = ")
					.append(df.format(this.fieldValues[i]) + "\n");

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

	public boolean isTheSame(StreamElement se){
//		Log.v(TAG, se.toString());
//		Log.v(TAG, this.toString());
		for (int i = 0; i < this.fieldNames.length; i++) {
			if (((Double) se.getData()[i] - (Double) this.getData()[i]) > 0.00001){
				Log.v(TAG, "Different: " + ((Double) se.getData()[i] - (Double) this.getData()[i]));
				return false;
			}
		}
//		Log.v(TAG, "Same");
		
		return true;
	}
}
