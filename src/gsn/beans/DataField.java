package gsn.beans;

import gsn.utils.GSNRuntimeException;

import java.io.Serializable;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch) <br>
 * Create : Apr 26, 2005 <br>
 */
public final class DataField implements Serializable {
   
   /**
    * 
    */
   private static final long serialVersionUID = -8841539191525018987L;
   
   private String            description      = "Not Provided";
   
   private String            fieldName;
   
   private int               dataTypeID       = -1;
   
   private String            type;
   
   private DataField ( ) {
   /**
    * This constructor is needed by JIBX XML Parser.
    */
   }
   
   public DataField ( final String fieldName , final String type , final String description ) throws GSNRuntimeException {
      this.fieldName = fieldName;
      this.type = type;
      this.dataTypeID = DataTypes.convertTypeNameToTypeID( type );
      this.description = description;
   }
   
   public DataField ( final String name , final String type ) {
      this.fieldName = name;
      this.type = type;
      this.dataTypeID = DataTypes.convertTypeNameToTypeID( type );
   }
   
   public String getDescription ( ) {
      return this.description;
   }
   
   public String getFieldName ( ) {
      return this.fieldName.toUpperCase( );
   }
   
   public boolean equals ( final Object o ) {
      if ( this == o ) return true;
      if ( !( o instanceof DataField ) ) return false;
      
      final DataField dataField = ( DataField ) o;
      if ( this.fieldName != null ? !this.fieldName.equals( dataField.fieldName ) : dataField.fieldName != null ) return false;
      return true;
   }
   
   /**
    * @return Returns the dataTypeID.
    */
   public int getDataTypeID ( ) {
      if ( this.dataTypeID == -1 ) this.dataTypeID = DataTypes.convertTypeNameToTypeID( this.type );
      return this.dataTypeID;
   }
   
   public int hashCode ( ) {
      return ( this.fieldName != null ? this.fieldName.hashCode( ) : 0 );
   }
   
   public String toString ( ) {
      final StringBuilder result = new StringBuilder( );
      result.append( "[Field-Name:" ).append( this.fieldName ).append( ", Type:" ).append( DataTypes.TYPE_NAMES[ this.getDataTypeID( ) ] ).append( "[" + this.type + "]" ).append( ", Decription:" )
            .append( this.description ).append( "]" );
      return result.toString( );
   }
   
   /**
    * @return Returns the type. This method is just used in the web interface
    * for detection the output of binary fields.
    */
   public String getType ( ) {
      return this.type;
   }
   
}
