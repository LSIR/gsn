package gsn.beans;

import gsn.utils.CaseInsensitiveComparator;

import java.io.Serializable;
import java.util.Collection;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public final class StreamElement implements Serializable {
   
   /**
    * 
    */
   private static final long                      serialVersionUID  = 2000261462783698617L;
   
   private static final transient Logger          logger            = Logger.getLogger( StreamElement.class );
   
   private transient TreeMap < String , Integer > indexedFieldNames = null;
   
   private long                                   timeStamp         = -1;
   
   private String [ ]                             fieldNames;
   
   private Serializable [ ]                       fieldValues;
   
   private Integer [ ]                            fieldTypes;
   
   public StreamElement ( final Collection < DataField > outputStructure , final Serializable [ ] data , final long timeStamp ) {
      this.fieldNames = new String [ outputStructure.size( ) ];
      this.fieldTypes = new Integer [ outputStructure.size( ) ];
      this.timeStamp = timeStamp;
      
      final DataField [ ] outputStructureArr = outputStructure.toArray( new DataField [ ] {} );
      for ( int i = 0 ; i < this.fieldNames.length ; i++ ) {
         this.fieldNames[ i ] = outputStructureArr[ i ].getFieldName( );
         this.fieldTypes[ i ] = outputStructureArr[ i ].getDataTypeID( );
      }
      if ( this.fieldNames.length != data.length ) throw new IllegalArgumentException( "The length of dataFileNames and the actual data provided in the constructor of StreamElement doesn't match." );
      this.verifyTypesCompatibility( this.fieldTypes , data );
      this.fieldValues = data;
      
   }
   
   public StreamElement ( final Collection < DataField > outputStructure , final Serializable [ ] data) {
      this(outputStructure,data,System.currentTimeMillis( ));
   }      
      
   public StreamElement ( final String [ ] dataFieldNames , final Integer [ ] dataFieldTypes , final Serializable [ ] data ) {
      this(dataFieldNames,dataFieldTypes,data,System.currentTimeMillis( ));
   }
   public StreamElement ( final String [ ] dataFieldNames , final Integer [ ] dataFieldTypes , final Serializable [ ] data , final long timeStamp ) {
      if ( dataFieldNames.length != dataFieldTypes.length ) throw new IllegalArgumentException(
         "The length of dataFileNames and dataFileTypes provided in the constructor of StreamElement doesn't match." );
      if ( dataFieldNames.length != data.length ) throw new IllegalArgumentException( "The length of dataFileNames and the actual data provided in the constructor of StreamElement doesn't match." );
      this.timeStamp = timeStamp;
      this.fieldTypes = dataFieldTypes;
      this.fieldNames = dataFieldNames;
      this.verifyTypesCompatibility( dataFieldTypes , data );
      this.fieldValues = data;
   }
   
   private void verifyTypesCompatibility ( final Integer [ ] fieldTypes , final Serializable [ ] data ) throws IllegalArgumentException {
      for ( int i = 0 ; i < data.length ; i++ ) {
         if ( data[ i ] == null ) continue;
         switch ( fieldTypes[ i ] ) {
            case DataTypes.SMALLINT :
               if ( !( data[ i ] instanceof Short ) ) throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ i ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" );
               break;
            case DataTypes.BIGINT :
               if ( !( data[ i ] instanceof Long ) ) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ i ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
               break;
            case DataTypes.CHAR :
            case DataTypes.VARCHAR :
               if ( !( data[ i ] instanceof String ) ) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ i ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
               break;
            case DataTypes.INTEGER :
               if ( !( data[ i ] instanceof Integer ) ) { throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ i ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" ); }
               break;
            case DataTypes.DOUBLE :
               if ( !( data[ i ] instanceof Double || data[ i ] instanceof Float ) ) throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 )
                  + "th field is defined as " + DataTypes.TYPE_NAMES[ i ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" );
               break;
            case DataTypes.BINARY :
               if ( data[ i ] instanceof String ) data[ i ] = ( ( String ) data[ i ] ).getBytes( );
               if ( !( data[ i ] instanceof byte [ ] ) ) throw new IllegalArgumentException( "The newly constructed Stream Element is not consistant. The " + ( i + 1 ) + "th field is defined as "
                  + DataTypes.TYPE_NAMES[ i ] + " while the actual data in the field is of type : *" + data[ i ].getClass( ).getCanonicalName( ) + "*" );
               break;
         }
      }
   }
   
   public String toString ( ) {
      final StringBuffer output = new StringBuffer( "TIMED = " );
      output.append( this.getTimeStamp( ) ).append( "\t" );
      for ( int i = 0 ; i < this.fieldNames.length ; i++ )
         output.append( "," ).append( this.fieldNames[ i ] ).append( "/" ).append( this.fieldTypes[ i ] ).append( " = " ).append( this.fieldValues[ i ] );
      return output.toString( );
   }
   
   public final String [ ] getFieldNames ( ) {
      return this.fieldNames;
   }
   
   public final Integer [ ] getFieldTypes ( ) {
      return this.fieldTypes;
   }
   
   public final Serializable [ ] getData ( ) {
      return this.fieldValues;
   }
   
   public long getTimeStamp ( ) {
      return this.timeStamp;
   }
   
   public StringBuilder getFieldTypesInString ( ) {
      final StringBuilder stringBuilder = new StringBuilder( );
      for ( final Integer i : this.getFieldTypes( ) )
         stringBuilder.append( DataTypes.TYPE_NAMES[ i ] ).append( " , " );
      return stringBuilder;
   }
   
   /**
    * Returns true if the timestamp is valid. A timestamp is valid if it is
    * above zero.
    * 
    * @return Whether the timestamp is valid or not.
    */
   public boolean isTimestampSet ( ) {
      return this.timeStamp > 0;
   }
   
   /**
    * Sets the time stamp of this stream element.
    * 
    * @param timeStamp The time stamp value. If the timestamp is zero or
    * negative, it is considered non valid and zero will be placed.
    */
   public void setTimeStamp ( long timeStamp ) {
      if ( this.timeStamp <= 0 ) timeStamp = 0;
      else
         this.timeStamp = timeStamp;
   }
   
   /**
    * This method gets the attribute name as the input and returns the value
    * corresponding to that tuple.
    * 
    * @param fieldName The name of the tuple.
    * @return The value corresponding to the named tuple.
    */
   public final Serializable getData ( final String fieldName ) {
      if ( this.indexedFieldNames == null ) {
         this.indexedFieldNames = new TreeMap < String , Integer >( new CaseInsensitiveComparator( ) );
         for ( int i = 0 ; i < this.fieldNames.length ; i++ )
            this.indexedFieldNames.put( this.fieldNames[ i ] , i );
      }
      return this.fieldValues[ this.indexedFieldNames.get( fieldName ) ];
   }
}
