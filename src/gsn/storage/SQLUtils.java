package gsn.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class SQLUtils {
   
   /**
    * This method gets a sql query and changes the table names using the
    * mappings provided in the second arguments.<br>
    * Example : select "a.*" from "a" , Mapping (a --> ali) result : select
    * "ali.*" from "ali"
    * 
    * @param sql
    * @param mapping (The keys should be in UPPER CASE)
    */
   public static String rewriteQuery ( String sql , HashMap < String , String > mapping ) {
      
      Pattern pattern1 = Pattern.compile( "(?<=\")(\\w+)(?=[\"])" , Pattern.CASE_INSENSITIVE );
      Pattern pattern2 = Pattern.compile( "(?<=\")(\\w+)(?=[\\.\"])" , Pattern.CASE_INSENSITIVE );
      StringBuffer result = new StringBuffer( );
      
      Matcher matcher = pattern2.matcher( sql );
      while ( matcher.find( ) ) {
         String replacement = mapping.get( matcher.group( 1 ).toUpperCase( ) );
         if ( replacement != null ) matcher.appendReplacement( result , replacement );
      }
      matcher.appendTail( result );
      
      matcher = pattern1.matcher( result.toString( ) );
      result = new StringBuffer( );
      while ( matcher.find( ) ) {
         String replacement = mapping.get( matcher.group( 1 ).toUpperCase( ) );
         if ( replacement != null ) matcher.appendReplacement( result , replacement );
      }
      matcher.appendTail( result );
      return result.toString( );
   }
   
   public static ArrayList < String > extractTableNamesUsedInQuery ( StringBuilder query ) {
      
      StringBuffer input = new StringBuffer( query.toString( ).toUpperCase( ) );
      int indexEndOfFrom = input.indexOf( " FROM " ) + " FROM".length( );
      input.delete( 0 , indexEndOfFrom );
      int indexOfWhere = input.indexOf( " WHERE " );
      int indexOfOrderBy = input.indexOf( " ORDER " );
      int indexOfGroupBy = input.indexOf( " GROUP " );
      int endIndex;
      if ( indexOfWhere > 0 ) endIndex = indexOfWhere;
      else if ( indexOfGroupBy > 0 ) endIndex = indexOfGroupBy;
      else if ( indexOfOrderBy > 0 ) endIndex = indexOfOrderBy;
      else
         endIndex = input.length( );
      input.delete( endIndex , input.length( ) );
      
      while ( input.indexOf( " AS " ) > 0 ) {
         int indexOfAs = input.indexOf( " AS " );
         int indexofComma = input.indexOf( "," , indexOfAs );
         if ( indexofComma == -1 ) {
            indexofComma = input.length( );
         }
         input.delete( indexOfAs , indexofComma );
      }
      ArrayList < String > result = new ArrayList < String >( );
      while ( input.indexOf( "," ) > 0 ) {
         String tableName = input.substring( 0 , input.indexOf( "," ) );
         tableName = tableName.trim( );
         result.add( tableName );
         input.delete( 0 , input.indexOf( "," ) + ",".length( ) );
      }
      if ( input.length( ) > 0 ) result.add( input.toString( ).trim( ) );
      return result;
      
   }
}
