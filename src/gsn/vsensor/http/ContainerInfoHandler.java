package gsn.vsensor.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.KeyValue;
import org.apache.commons.lang.StringEscapeUtils;


/**
 * @author alisalehi
 *
 */
public class ContainerInfoHandler implements RequestHandler{

   public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
      response.setStatus( HttpServletResponse.SC_OK );
      Iterator < VSensorConfig > vsIterator = Mappings.getAllVSensorConfigs( );
      StringBuilder sb = new StringBuilder( "\n<gsn " );
      sb.append( "name=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebName( ) ) ).append( "\" " );
      sb.append( "author=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebAuthor( ) ) ).append( "\" " );
      sb.append( "email=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebEmail( ) ) ).append( "\" " );
      sb.append( "description=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebDescription( ) ) ).append( "\" >\n" );
      while ( vsIterator.hasNext( ) ) {
         VSensorConfig sensorConfig = vsIterator.next( );
         sb.append( "<virtual-sensor name=\"" ).append( sensorConfig.getVirtualSensorName( ) ).append( "\"").append( " last-modified=\"" ).append( new File( sensorConfig.getFileName( ) ).lastModified( ) ).append( "\"").append( " >\n" );
         for ( DataField df : sensorConfig.getOutputStructure( ) )
            sb.append( "<field name=\"" ).append( df.getFieldName( ) ).append( "\" " ).append( "type=\"" ).append( df.getType( ) ).append( "\" " ).append( "description=\"" ).append(
               StringEscapeUtils.escapeXml( df.getDescription( ) ) ).append( "\" />\n" );
         sb.append( "<field name=\"TIMED\" type=\"long\" description=\"The timestamp associated with the stream element\" />\n" );
         for ( KeyValue df : sensorConfig.getAddressing( ) )
            sb.append( "<predicate key=\"" ).append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ) ) ).append( "\">" ).append( StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ) ).append( "</predicate>\n" );
         StringBuilder query = new StringBuilder( "select * from " + sensorConfig.getVirtualSensorName( ) +" order by TIMED limit 1 offset 0" );
         Enumeration < StreamElement > result = StorageManager.getInstance( ).executeQuery( query , true );
         while ( result.hasMoreElements( ) ) {
            StreamElement se = result.nextElement( );
            sb.append( "<stream-element>\n" );
            for ( int i = 0 ; i < se.getFieldNames( ).length ; i++ )
               if ( se.getFieldTypes( )[ i ] == DataTypes.BINARY )
                  sb.append( "<field name=\"" ).append( se.getFieldNames( )[ i ] ).append( "\">" ).append( se.getData( )[ i ].toString( ) ).append( "</field>\n" );
               else
                  sb.append( "<field name=\"" ).append( se.getFieldNames( )[ i ] ).append( "\">" ).append( StringEscapeUtils.escapeXml( se.getData( )[ i ].toString( ) ) ).append( "</field>\n" );
            sb.append( "<field name=\"TIMED\" >" ).append( se.getTimeStamp( ) ).append( "</field>\n" );
            sb.append( "</stream-element>\n" );
         }
         sb.append( "</virtual-sensor>\n" );
      }
      sb.append( "</gsn>\n" );
      response.getWriter( ).write( sb.toString( ) );
      
   }

   public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
      return true;
   }
}
