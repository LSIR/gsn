package gsn.beans;

import gsn.storage.DataEnumerator;
import gsn.wrappers.Wrapper;

import java.io.Serializable;
import java.util.TreeMap;
import java.util.HashMap;

import org.apache.log4j.Logger;


public class StreamElement implements Serializable {

  private static final long                      serialVersionUID  = 2000261462783698617L;

  private static final transient Logger logger            = Logger.getLogger( StreamElement.class );

  private transient TreeMap < String , Integer > indexedFieldNames = null;

  private Long timed = null;

  private HashMap<String,Byte> fieldNameToTypeMapping = new HashMap<String, Byte>();

  private HashMap<String,Serializable> fieldNameToValueMapping = new HashMap<String, Serializable>();

  private transient Long                                   guidinternalPrimayKey = null; //used to avoid duplicates.

  private void verifyTypesCompatibility ( final Byte [ ] fieldTypes , final Serializable [ ] data ) throws IllegalArgumentException {
    for ( int i = 0 ; i < data.length ; i++ ) {
      if ( data[ i ] == null ) continue;
      switch ( fieldTypes[ i ] ) {
        case DataTypes.TINYINT :
          if ( !( data[ i ] instanceof Byte ) )
            throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as " + DataTypes.TYPE_NAMES[ fieldTypes[i] ]
                    + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" );
          break;
        case DataTypes.SMALLINT :
          if ( !( data[ i ] instanceof Short ) )
            throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as " + DataTypes.TYPE_NAMES[ fieldTypes[i] ]
                    + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" );
          break;
        case DataTypes.BIGINT :
          if ( !( data[ i ] instanceof Long ) ) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ fieldTypes[i] ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
          break;
        case DataTypes.CHAR :
        case DataTypes.VARCHAR :
          if ( !( data[ i ] instanceof String ) ) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ fieldTypes[i] ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
          break;
        case DataTypes.INTEGER :
          if ( !( data[ i ] instanceof Integer)) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ fieldTypes[i] ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
          break;
        case DataTypes.DOUBLE :
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


  public Long getTimed( ) {
    return this.timed;
  }

  /**
   * Returns true if the timestamp is valid. A timestamp is valid if it is
   * above zero.
   *
   * @return Whether the timestamp is valid or not.
   */
  public boolean isTimestampSet ( ) {
    return getTimed()!=null;
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
    if (name.toLowerCase().equals("timed"))
      setTime(Long.parseLong(value.toString()));
    else if (fieldNameToValueMapping.containsKey(name.toLowerCase()))
      fieldNameToValueMapping.put(name.toLowerCase(),value);
    return this;
  }

  public Serializable getValue(String name)	{
    if (name.toLowerCase().equals("timed"))
      return getTimed();
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

  public StreamElement setTime(Long timestamp) {
    this.timed =timestamp;
    return this;
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
