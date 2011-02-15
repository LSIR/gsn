package gsn.http;

import gsn.Container;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.VSensorConfig;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

public class OutputStructureHandler implements RequestHandler{
   private static transient Logger                                      logger                             = Logger.getLogger( OutputStructureHandler.class );
   
   public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
      response.setStatus( HttpServletResponse.SC_OK );
      String vsName = request.getParameter( "name" );
      VSensorConfig sensorConfig = Mappings.getVSensorConfig( vsName );
      if ( logger.isInfoEnabled( ) ) logger.info( new StringBuilder( ).append( "Structure request for *" ).append( vsName ).append( "* received." ).toString( ) );
      StringBuilder sb = new StringBuilder("<virtual-sensor name=\"" ).append( vsName ).append( "\">\n" );
      for ( DataField df : sensorConfig.getOutputStructure( ) )
         sb.append( "<field name=\"" ).append( df.getName( ) ).append( "\" " ).append( "type=\"" ).append( df.getType( ) ).append( "\" " ).append( "description=\"" ).append(
            StringEscapeUtils.escapeXml( df.getDescription( ) ) ).append( "\" />\n" );
      sb.append( "<field name=\"timed\" type=\"string\" description=\"The timestamp associated with the stream element\" />\n" );
      sb.append( "</virtual-sensor>" );
      response.getWriter( ).write( sb.toString( ) );
   }

   public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
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
