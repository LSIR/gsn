package gsn.storage;

import gsn.utils.CaseInsensitiveComparator;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 */
public class SQLUtils {
   
   
   public static ArrayList < CharSequence > extractTableNamesUsedInQuery ( CharSequence query ) {
      
      StringBuffer input = new StringBuffer( query.toString( ).toLowerCase() );
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
      ArrayList < CharSequence > result = new ArrayList < CharSequence >( );
      while ( input.indexOf( "," ) > 0 ) {
         String tableName = input.substring( 0 , input.indexOf( "," ) );
         tableName = tableName.trim( );
         result.add( tableName );
         input.delete( 0 , input.indexOf( "," ) + ",".length( ) );
      }
      if ( input.length( ) > 0 ) result.add( input.toString( ).trim( ) );
      return result;
      
   }
   /**
    * Table renaming, note that the renameMapping should be a tree map.
    * 
    * This method gets a sql query and changes the table names using the
    * mappings provided in the second arguments.<br>
    *    
    * @param query
    * @param renameMapping
    * @return
    */
   public static CharSequence newRewrite(CharSequence query, TreeMap<CharSequence,CharSequence> renameMapping){
	   // Selecting strings between pair of "" : (\"[^\"]*\")
	   // Selecting tableID.tableName  or tableID.* : (\\w+(\\.(\w+)|\\*))
	   // The combined pattern is : (\"[^\"]*\")|(\\w+\\.((\\w+)|\\*))
	   Pattern pattern = Pattern.compile( "(\"[^\"]*\")|((\\w+)(\\.((\\w+)|\\*)))" , Pattern.CASE_INSENSITIVE );
       Matcher matcher = pattern.matcher( query );
       StringBuffer result = new StringBuffer( );
       if (!(renameMapping.comparator() instanceof CaseInsensitiveComparator))
    	   throw new RuntimeException("Query rename needs case insensitive treemap.");
       while ( matcher.find( ) ) {
    	   if (matcher.group(2)==null)
    		   continue;
    	   String tableName = matcher.group(3);
         // System.out.println(matcher.group(3));
    	   CharSequence replacement = renameMapping.get(tableName );
    	   // $4 means that the 4th group of the match should be appended to the string (the forth group contains the field name).
          if ( replacement != null ) matcher.appendReplacement( result , new StringBuilder(replacement).append("$4").toString() );
       }
       String toReturn= matcher.appendTail( result ).toString().toLowerCase();
       int indexOfFrom = toReturn.indexOf(" from ")+" from ".length();
       System.out.println(toReturn);
       int indexOfWhere =( toReturn.lastIndexOf(" where ")>0?(toReturn.lastIndexOf(" where ")):toReturn.length());
       String selection = toReturn.substring(indexOfFrom,indexOfWhere);
       Pattern fromClausePattern = Pattern.compile( "\\s*(\\w+)\\s*" , Pattern.CASE_INSENSITIVE );
       Matcher fromClauseMather = fromClausePattern.matcher( selection );
       result = new StringBuffer();
       while ( fromClauseMather.find( ) ) {
    	   if (fromClauseMather.group(1)==null)
    		   continue;
    	   String tableName = fromClauseMather.group(1);
    	   CharSequence replacement = renameMapping.get(tableName );
           if ( replacement != null ) fromClauseMather.appendReplacement( result , replacement.toString() );
       }
       String cleanFromClause= fromClauseMather.appendTail( result ).toString();
      String finalResult = StringUtils.replace(toReturn, selection, cleanFromClause);
       return finalResult;   
   }

   public static void main(String[] args) {
	   TreeMap< CharSequence, CharSequence> map = new TreeMap<CharSequence, CharSequence>(new CaseInsensitiveComparator());
	   map.put("x", "done");
	CharSequence out = newRewrite("select ali.fd x.x fdfd.fdfd r.*, * from x,x, bla, x where k", map);
	
	System.out.println(out.toString());
}
}
