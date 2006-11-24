package gsn.vsensor.http;

import gsn.Container;
import gsn.Mappings;
import gsn.beans.VSensorConfig;
import java.io.File;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.collections.KeyValue;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;


/**
 * @author alisalehi
 */
public class AddressingReqHandler implements RequestHandler {
   
   private static transient Logger                                      logger                             = Logger.getLogger( AddressingReqHandler.class );
   
   public void handle ( HttpServletRequest request,HttpServletResponse response ) throws IOException {
      response.setStatus( HttpServletResponse.SC_OK );
      String vsName = request.getParameter( "name" );
      VSensorConfig sensorConfig = Mappings.getVSensorConfig( vsName );
      if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Structure request for *" ).append( vsName ).append( "* received." ).toString( ) );
      StringBuilder sb = new StringBuilder( "<virtual-sensor name=\"" ).append( vsName ).append( "\" last-modified=\"" ).append( new File( sensorConfig.getFileName( ) ).lastModified( ) ).append( "\">\n" );
      for ( KeyValue df : sensorConfig.getAddressing( ) )
         sb.append( "<predicate key=\"" ).append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ) ) ).append( "\">" ).append( StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ) )
               .append( "</predicate>\n" );
      sb.append( "</virtual-sensor>" );
      response.getWriter( ).write( sb.toString( ) );
   }

   public boolean isValid ( HttpServletRequest request,HttpServletResponse response ) throws IOException {
      String vsName = request.getParameter( "name" );
      if ( vsName == null || vsName.trim( ).length( )==0 ) {
         response.sendError( Container.MISSING_VSNAME_ERROR , "The virtual sensor name is missing" );
         return false;
      }
      VSensorConfig sensorConfig = Mappings.getVSensorConfig( vsName );
      if ( sensorConfig == null ) {
         response.sendError( Container.ERROR_INVALID_VSNAME , "The specified virtual sensor doesn't exist." );
         return false;
      }
      return true;
   }
}
