package gsn.beans;

import gsn.storage.DataEnumerator;
import gsn2.wrappers.Wrapper;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;


public class StreamElement implements Serializable {

    private static final long                      serialVersionUID  = 2000261462783698617L;

    private static final transient Logger logger            = Logger.getLogger( StreamElement.class );

    private transient TreeMap < String , Integer > indexedFieldNames = null;

    private DateTime timed = null;

    private HashMap<String,Byte> fieldNameToTypeMapping = new HashMap<String, Byte>();

    private HashMap<String,Serializable> fieldNameToValueMapping = new HashMap<String, Serializable>();

    private transient Long                                   guidinternalPrimayKey = null; //used to avoid duplicates.

    private void verifyTypesCompatibility ( final Byte [ ] fieldTypes , final Serializable [ ] data ) throws IllegalArgumentException {
        for ( int i = 0 ; i < data.length ; i++ ) {
            if ( data[ i ] == null ) continue;
            switch ( fieldTypes[ i ] ) {
                case DataTypes.TIME :
                    if ( !( data[ i ] instanceof Long ) ) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                            + DataTypes.TYPE_NAMES[ fieldTypes[i] ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
                    break;
                case DataTypes.STRING:
                    if ( !( data[ i ] instanceof String ) ) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                            + DataTypes.TYPE_NAMES[ fieldTypes[i] ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
                    break;
                case DataTypes.NUMERIC:
                    if ( !( data[ i ] instanceof Double || data[ i ] instanceof Float ) )
                        throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as " + DataTypes.TYPE_NAMES[ fieldTypes[i] ]
                                + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" );
                    break;
                case DataTypes.BINARY :
                    // if ( data[ i ] instanceof String ) data[ i ] = ( ( String )
                    // data[ i ] ).getBytes( );
                    if ( !( data[ i ] instanceof byte [ ] || data[ i ] instanceof String ) )
                        throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as " + DataTypes.TYPE_NAMES[ fieldTypes[i] ]
                                + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" );
                    break;
            }
        }
    }

    public final String [ ] getFieldNames ( ) {
        return fieldNameToTypeMapping.keySet().toArray(new String[]{});
    }


    public DateTime getTime( ) {
        return this.timed;
    }

    public Long getTimeInMillis( ) {
        if (isTimestampSet())
            return getTime().getMillis();
        return null;
    }


    /**
     * Returns true if the timestamp is valid. A timestamp is valid if it is
     * above zero.
     *
     * @return Whether the timestamp is valid or not.
     */
    public boolean isTimestampSet ( ) {
        return getTime()!=null;
    }

    public long getInternalPrimayKey ( ) {
        return guidinternalPrimayKey;
    }

    public void setInternalPrimayKey ( Long internalPrimayKey ) {
        this.guidinternalPrimayKey = internalPrimayKey;
    }

    protected StreamElement(DataField[] fields) {
        for(DataField field :fields ){
            fieldNameToTypeMapping.put(field.getName().toLowerCase(),field.getDataTypeID());
            fieldNameToValueMapping.put(field.getName().toLowerCase(),null);
        }
    }

    public static StreamElement from(Operator config) {
        StreamElement se = new StreamElement(config.getStructure());
        return se;
    }

    public static StreamElement from(Wrapper wrapper) {
        StreamElement se = new StreamElement(wrapper.getOutputFormat());
        return se;
    }
    public StreamElement set(String name,Serializable value){
        if (name.toLowerCase().equals("timed")){
            if (value instanceof DateTime)
                setTime((DateTime)value);
            else //it is long
                setTime(new DateTime(((Number) value).longValue()));
        }
        else if (fieldNameToValueMapping.containsKey(name.toLowerCase()))
            fieldNameToValueMapping.put(name.toLowerCase(),value);
        return this;
    }

    public Serializable getValue(String name)	{
        if (name.toLowerCase().equals("timed"))
            return getTimeInMillis();
        Serializable toReturn = fieldNameToValueMapping.get(name.toLowerCase());
        if(toReturn ==null)
            logger.info("There is a request for field "+name+" for StreamElement: "+this.toString()+". As the requested field doesn't exist, GSN returns Null to the callee.");
        return toReturn;
    }

    public Byte getType(String name)	{
        Byte toReturn = fieldNameToTypeMapping.get(name.toLowerCase());
        if(toReturn ==null)
            logger.info("There is a request for type field "+name+" for StreamElement: "+this.toString()+". As the requested field doesn't exist, GSN returns Null to the callee.");
        return toReturn;
    }

    public StreamElement setTime(DateTime timestamp) {
        this.timed =timestamp;
        return this;
    }

     public StreamElement setTime(long milliSeconds) {
        this.timed =new DateTime(milliSeconds);
        return this;
    }

    public boolean isEmpty() {
        for(Object o : fieldNameToValueMapping.values())
            if (o!=null)
                return false;
        return !isTimestampSet();

    }

    private class DBStreamElemet extends StreamElement{
        public DBStreamElemet(DataEnumerator e) {
            super(null);
        }
    }

    public String toString() {
        return "StreamElement{" +
                "indexedFieldNames=" + indexedFieldNames +
                ", timed=" + timed +
                ", fieldNameToTypeMapping=" + fieldNameToTypeMapping +
                ", fieldNameToValueMapping=" + fieldNameToValueMapping +
                ", guidinternalPrimayKey=" + guidinternalPrimayKey +
                '}';
    }
}
