package gsn.vsensor.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * @author alisalehi
 */
public class ContainerInfoHandler implements RequestHandler {
   
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
         sb.append( "<virtual-sensor name=\"" ).append( sensorConfig.getVirtualSensorName( ) ).append( "\"" ).append( " last-modified=\"" ).append(
            new File( sensorConfig.getFileName( ) ).lastModified( ) ).append( "\"" ).append( " >\n" );
         StringBuilder query = new StringBuilder( "select * from " + sensorConfig.getVirtualSensorName( ) + " order by TIMED limit 1 offset 0" );
         DataEnumerator result = StorageManager.getInstance( ).executeQuery( query , true );
         StreamElement se = null;
         if ( result.hasMoreElements( ) ) se = result.nextElement( );
         for ( DataField df : sensorConfig.getOutputStructure( ) ) {
            sb.append( "<field name=\"" ).append( df.getFieldName( ) ).append( "\" " ).append( "type=\"" ).append( df.getType( ) ).append( "\" " );
            if ( df.getDescription( ) != null && df.getDescription( ).trim( ).length( ) != 0 )
               sb.append( "description=\"" ).append( StringEscapeUtils.escapeXml( df.getDescription( ) ) ).append( "\"" );
            sb.append( " >" );
            if ( se != null ) if ( df.getType( ).toLowerCase( ).trim( ).indexOf( "binary" ) > 0 )
               sb.append( se.getData( df.getFieldName( ) ) );
            else
               sb.append( se.getData( StringEscapeUtils.escapeXml( df.getFieldName( ) ) ) );
            sb.append( "</field>" );
         }
         result.close( );
         sb.append( "<field name=\"TIMED\" type=\"long\" description=\"The timestamp associated with the stream element\" >" ).append( se == null ? "" : se.getTimeStamp( ) ).append( "</field>\n" );
         for ( KeyValue df : sensorConfig.getAddressing( ) )
            sb.append( "<field name=\"" ).append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ) ) ).append( "\" type=\"predicate\" >" ).append(
               StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ) ).append( "</field>\n" );
         sb.append( "</virtual-sensor>\n" );
      }
     
      sb.append( "</gsn>\n" );
      
      response.getWriter( ).write( sb.toString( ) );
   }
   
   public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
      return true;
   }
}
