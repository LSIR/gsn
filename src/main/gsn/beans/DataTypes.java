package gsn.beans;

import gsn.utils.GSNRuntimeException;

import java.sql.Types;
import java.util.Date;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class DataTypes {
   
   public final static String            OPTIONAL_NUMBER_PARAMETER = "\\s*(\\(\\s*\\d+\\s*\\))?";
   
   public final static String            REQUIRED_NUMBER_PARAMETER = "\\s*\\(\\s*\\d+\\s*\\)";
   
   private final static transient Logger logger                    = Logger.getLogger( DataTypes.class );
   
   // NEXT FIELD
   public final static String STRING_PATTERN_STRING = "\\s*char" + REQUIRED_NUMBER_PARAMETER + "\\s*";
   
   public final static byte STRING = 1;
   
   public final static String STRING_NAME = "String";
   
   // NEXT FIELD
   public final static String            BINARY_PATTERN_STRING     = "\\s*(BINARY|BLOB)" + OPTIONAL_NUMBER_PARAMETER + "(\\s*:.*)?";
   
   public final static byte               BINARY                    = 4;
   
   public final static String            BINARY_NAME               = "Binary";
   
   // NEXT FIELD
   public final static String NUMERIC_PATTERN_STRING = "\\s*NUMERIC\\s*";
   
   public final static byte NUMERIC = 5;
   
   public final static String NUMERIC_NAME = "Numeric";
   
   // NEXT FIELD
   /**
    * Type Time is not supported at the moment. If you want to present time, please use
    * longint. For more information consult the GSN mailing list on the same subject. 
    */
   public final static String            TIME_PATTERN_STRING       = "\\s*TIME\\s*";
   
   public final static byte               TIME                      = 6;
  
   public final static String            TIME_NAME                 = "Time";
   
   // FINISH
   public final static Pattern [ ]       ALL_PATTERNS              = new Pattern [ ] {
         Pattern.compile(STRING_PATTERN_STRING, Pattern.CASE_INSENSITIVE ),
         Pattern.compile( BINARY_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) ,
         Pattern.compile(NUMERIC_PATTERN_STRING, Pattern.CASE_INSENSITIVE ) ,
         Pattern.compile( TIME_PATTERN_STRING , Pattern.CASE_INSENSITIVE ) };
   
   public final static StringBuilder     ERROR_MESSAGE             = new StringBuilder( "Acceptable types are (TINYINT, SMALLINT, INTEGER,BIGINT,STRING(#),BINARY[(#)],VARCHAR(#),NUMERIC,TIME)." );
   
   public final static String [ ]        TYPE_NAMES                = new String [ ] {STRING_NAME,  BINARY_NAME , NUMERIC_NAME,  TIME_NAME };
   
   public final static Object [ ]        TYPE_SAMPLE_VALUES        = { "A chain of chars" , 'c' , new Integer( 32 ) , new Integer( 66000 ) , new Byte( ( byte ) 12 ) , new Double( 3.141592 ) ,
         new Date( ).getTime( ) , new Integer( 1 ) , new Integer( 9 ) };

    public static byte convertTypeNameToGSNTypeID ( final String type ) {
       if ( type == null ) throw new GSNRuntimeException( new StringBuilder( "The type *null* is not recoginzed by GSN." ).append( DataTypes.ERROR_MESSAGE ).toString( ) );
       if(type.trim().equalsIgnoreCase("string")) return DataTypes.STRING;
       for ( byte i = 0 ; i < DataTypes.ALL_PATTERNS.length ; i++ )
         if ( DataTypes.ALL_PATTERNS[ i ].matcher( type ).matches( ) ){
             return i;
         }
      if(type.trim().equalsIgnoreCase("numeric")) return DataTypes.NUMERIC;
      
      
      throw new GSNRuntimeException( new StringBuilder( "The type *" ).append( type ).append( "* is not recognized." ).append( DataTypes.ERROR_MESSAGE ).toString( ) );
   }
   
   /**
    * throws runtime exception if the type conversion fails.
    * @param sqlType
    * @return
    */
   public static byte SQLTypeToGSNTypeSimplified(int sqlType) {
	   if (sqlType == Types.BIGINT || sqlType == Types.SMALLINT || sqlType == Types.DOUBLE || sqlType==Types.INTEGER || sqlType == Types.DECIMAL||sqlType == Types.REAL || sqlType == Types.FLOAT|| sqlType == Types.NUMERIC )
			return  DataTypes.NUMERIC;
		else if (sqlType == Types.VARCHAR || sqlType == Types.CHAR|| sqlType == Types.LONGNVARCHAR || sqlType == Types.LONGVARCHAR || sqlType== Types.NCHAR )
			return  DataTypes.STRING;
		else if (sqlType == Types.BINARY || sqlType == Types.BLOB|| sqlType == Types.VARBINARY )
			return  DataTypes.BINARY;
	   throw new RuntimeException("Can't convert SQL type id of: "+sqlType+ " to GSN type id.");
   }
}
