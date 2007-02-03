package gsn.web;

import gsn.Container;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

//path="/WEB-INF/file_not_found.jpg"
/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Create : 2006 <br>
 * Created for : GSN project. <br>
 * @web.servlet name="BinaryDownload" load-on-startup="true"
 * @web.servlet-mapping url-pattern="/field"
 */
public class FieldDownloadServlet extends HttpServlet {
   
   private static transient Logger                                      logger                             = Logger.getLogger( FieldDownloadServlet.class );
   
   static final String prefix  = "select * from ";
   
   static final String postfix = " where PK = ? ";
   
   public void doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
      String vsName = req.getParameter( "vs" );
      if ( vsName == null || (vsName =vsName.trim( ).toLowerCase()).length( ) == 0 ) {
         res.sendError( Container.MISSING_VSNAME_ERROR , "The virtual sensor name is missing" );
         return;
      }
      String primaryKey = req.getParameter( "pk" );
      String colName = req.getParameter( "field" );
      if ( primaryKey == null || colName == null || primaryKey.trim( ).length( ) == 0 || colName.trim( ).length( ) == 0 ) {
         res.sendError( res.SC_BAD_REQUEST , "The pk and/or field parameters are missing." );
         return;
      }
      VSensorConfig sensorConfig = Mappings.getVSensorConfig( vsName );
      if ( sensorConfig == null ) {
         res.sendError( Container.ERROR_INVALID_VSNAME , "The specified virtual sensor doesn't exist." );
         return;
      }
      
      primaryKey = primaryKey.trim( );
      colName = colName.trim( );
      // TODO : Check to see if the requested column exists.
      StringBuilder query = new StringBuilder( ).append( prefix ).append( vsName ).append( postfix );
      ResultSet rs = StorageManager.getInstance( ).getBinaryFieldByQuery( query , colName , Long.parseLong( primaryKey ) );
      if ( rs == null ) {
         res.sendError( res.SC_NOT_FOUND , "The requested data is marked as obsolete and is not available." );
         return;
      }
      boolean binary = false;
      for ( DataField df : sensorConfig.getOutputStructure( ) )
         if ( df.getName( ).toLowerCase( ).equals( colName.trim( ).toLowerCase( ) ) ) if ( df.getDataTypeID( ) == DataTypes.BINARY ) {
            StringTokenizer st = new StringTokenizer( df.getType( ) , ":" );
            binary = true;
            if ( st.countTokens( ) != 2 ) break;
            st.nextToken( );// Ignoring the first token.
            res.setContentType( st.nextToken( ) );
            // if ( type.equalsIgnoreCase( "svg" ) ) res.setContentType( "" );
         }
      try {
         if ( binary )
            res.getOutputStream( ).write( rs.getBytes( colName ) );
         else {
            res.setContentType( "text/xml" );
            res.getWriter( ).write( rs.getString( colName ) );
         }
      } catch ( Exception e ) {
         logger.info( e.getMessage( ) , e );
      } finally {
         if ( rs != null ) try {
            rs.getStatement( ).getConnection( ).close( );
         } catch ( SQLException e ) {
            e.printStackTrace( );
         }
      }
   }
   
   public void doPost ( HttpServletRequest request , HttpServletResponse response ) throws ServletException , IOException {
      doGet( request , response );
   }
   
}
