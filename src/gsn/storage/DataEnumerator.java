package gsn.storage ;

import gsn.beans.DataTypes ;
import gsn.beans.StreamElement ;
import gsn.wrappers.StreamProducer ;
import java.io.Serializable ;
import java.sql.ResultSet ;
import java.sql.SQLException ;
import java.util.Enumeration ;
import java.util.Vector ;
import org.apache.log4j.Logger ;

/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * FIXME : 1. Because a prepared statements relies on the connection being there.
 * If a connection times out the pool will restore it and then the prepared statement is then stale.
 * The only way you can tell this it to use it, and any speed advantage is negated when you have to try it,
 * catch the error, regen the statement and go again.
 * 
 * 2. You use a connection pool in threaded enviroment. A prepared statement is a single instance that
 * can only be used in a single thread. So either you block all the threads waiting for the single prepared
 * statement (and then you have to wonder why you are using threads) or you have multiple prepared statements for same thing.
 * 
 */
public class DataEnumerator implements Enumeration {

   private transient Logger logger = Logger.getLogger ( DataEnumerator.class ) ;

   private ResultSet resultSet = null ;

   private String [ ] dataFieldNames ;

   private Integer [ ] dataFieldTypes ;

   private boolean hasNext = false ;

   boolean hasTimedFieldInResultSet = false ;

   int indexOfTimedField = - 1 ;

   int indexofPK = - 1 ;

   public DataEnumerator ( ) {
      hasNext = false ;
   }

   public DataEnumerator ( ResultSet rs ) throws SQLException {
      if ( rs == null )
         throw new IllegalStateException ( "The provided ResultSet is Null." ) ;

      this.resultSet = rs ;
      hasNext = resultSet.next ( ) ;
      Vector < String > fieldNames = new Vector < String > ( ) ;
      Vector < Integer > fieldTypes = new Vector < Integer > ( ) ;

      // Initializing the fieldNames and fieldTypes.
      // Also setting the values for <code> hasTimedFieldInResultSet</code>
      // if the TIMED field is present in the result set.
      for ( int i = 1 ; i <= resultSet.getMetaData ( ).getColumnCount ( ) ; i ++ ) {
         String colName = resultSet.getMetaData ( ).getColumnName ( i ) ;
         int colTypeInJDBCFormat = resultSet.getMetaData ( ).getColumnType ( i ) ;
         if ( colName.equalsIgnoreCase ( "PK" ) ) {
            indexofPK = i ;
         } else if ( colName.equalsIgnoreCase ( StreamProducer.TIME_FIELD ) ) {
            indexOfTimedField = i ;
         } else {
            fieldNames.add ( colName ) ;
            fieldTypes.add ( DataTypes.convertFromJDBCToGSNFormat ( colTypeInJDBCFormat ) ) ;
         }
      }
      dataFieldNames = fieldNames.toArray ( new String [ ] {} ) ;
      dataFieldTypes = fieldTypes.toArray ( new Integer [ ] {} ) ;

   }

   private StreamElement streamElement = null ;

   public boolean hasMoreElements ( ) {
      return hasNext ;
   }

   public StreamElement nextElement ( ) {
      if ( hasNext == false )
         return null ;
      try {
         Serializable [ ] output = new Serializable [ dataFieldNames.length ] ;
         for ( int i = 1 , colIndex = 0 ; i <= resultSet.getMetaData ( ).getColumnCount ( ) ; i ++ ) {
            if ( i == indexOfTimedField || i == indexofPK )
               continue ;
            switch ( dataFieldTypes [ colIndex ] ) {
            case DataTypes.VARCHAR :
            case DataTypes.CHAR :
               output [ colIndex ] = resultSet.getString ( i ) ;
               break ;
            case DataTypes.INTEGER :
               output [ colIndex ] = resultSet.getInt ( i ) ;
               break ;
            case DataTypes.TINYINT :
               output [ colIndex ] = resultSet.getByte ( i ) ;
               break ;
            case DataTypes.SMALLINT :
               output [ colIndex ] = resultSet.getShort ( i ) ;
               break ;
            case DataTypes.DOUBLE :
               output [ colIndex ] = resultSet.getDouble ( i ) ;
               break ;
            case DataTypes.BIGINT :
               output [ colIndex ] = resultSet.getLong ( i ) ;
               break ;
            case DataTypes.BINARY :
               output [ colIndex ] = resultSet.getBytes ( i ) ;
               break ;
            }
            colIndex ++ ;
         }
         if ( hasTimedFieldInResultSet )
            streamElement = new StreamElement ( dataFieldNames , dataFieldTypes , output , resultSet.getLong ( StreamProducer.TIME_FIELD ) ) ;
         else
            streamElement = new StreamElement ( dataFieldNames , dataFieldTypes , output , System.currentTimeMillis ( ) ) ;
         hasNext = resultSet.next ( ) ;
         if (hasNext==false)
             resultSet.getStatement ( ).getConnection ( ).close ( );
      } catch ( SQLException e ) {
         logger.error ( e.getMessage ( ) , e ) ;
         try {
            resultSet.close ( ) ;
         } catch ( SQLException e1 ) {
         }
      }
      return streamElement ;
   }
}
