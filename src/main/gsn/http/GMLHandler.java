package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.StreamElement;
import gsn.beans.VSFile;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class GMLHandler implements RequestHandler {

	private static transient Logger logger             = Logger.getLogger( GMLHandler.class );

	public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		response.setStatus( HttpServletResponse.SC_OK );
		String reqName = request.getParameter("name");

		response.getWriter( ).write( buildOutput(reqName) );
	}

	//return only the requested sensor if specified (otherwise use null)
	public String buildOutput (String reqName) {
		SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());

		StringBuilder outsb = new StringBuilder( "<gsn:FeatureCollection xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"./gsn.xsd\" xmlns:gsn=\"http://gsn.ch/\" xmlns:gml=\"http://www.opengis.net/gml\"> \n" );

		Iterator < VSFile > vsIterator = Mappings.getAllVSensorConfigs( );


		while ( vsIterator.hasNext( ) ) {
			VSFile sensorConfig = vsIterator.next( );
			if ( reqName != null && !sensorConfig.getName().equals(reqName) ) continue;
			StringBuilder sb = new StringBuilder();
			//      sb.append("<gml:featureMember>\n");
			//      sb.append("<gsn:sensors");
			//      sb.append(" fid=\"").append(sensorConfig.getName()).append("\"" );
			//      sb.append( ">\n" );
			//
			//      ArrayList<StreamElement> ses = getMostRecentValueFor(sensorConfig.getName());
			//      int counter = 1;
			//      if (ses!=null ) {
			//        for (StreamElement se:ses){
			//        	String lat = null;
			//        	String lon = null;
			//            for ( KeyValue df : sensorConfig.getAddressing( )){
			//            	if (StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase()).contentEquals("latitude"))
			//            		lat = new String ( StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ));
			//            	if (StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase()).contentEquals("longitude"))
			//            		lon = new String ( StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ));
			//              }
			//            if (lat !=null && lon!=null)
			//            	sb.append("\t<gsn:geometryProperty><gml:Point><gml:coordinates>").append(lon).append(",").append(lat).append( "</gml:coordinates></gml:Point></gsn:geometryProperty>\n");
			//            else break;
			//        	sb.append("\t<gsn:sensor>").append(sensorConfig.getName()).append("</gsn:sensor>\n");
			//            for ( KeyValue df : sensorConfig.getAddressing( )){
			//                sb.append("\t<gsn:").append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase()) ).append( ">");
			//                sb.append(StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ) );
			//                sb.append("</gsn:").append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase()) ).append( ">\n");
			//              }
			//          for ( DataField df : sensorConfig.getProcessingClassConfig().getOutputFormat() ) {
			//            sb.append("\t<gsn:").append(df.getName().toLowerCase()).append(">");
			//            if (se!= null )
			//              if (df.getType().toLowerCase( ).trim( ).indexOf( "binary" ) > 0 )
			//                sb.append( se.getData( df.getName( ) ) );
			//              else
			//                sb.append( se.getData( StringEscapeUtils.escapeXml( df.getName( ) ) ) );
			//            sb.append("</gsn:").append(df.getName().toLowerCase()).append(">\n");
			//          }
			//          counter++;
			//        }
			//      }
			//      sb.append( "</gsn:sensors>\n" );
			//      sb.append("</gml:featureMember>\n");
			//      outsb.append(sb);
		}
		//
		//    outsb.append( "</gsn:FeatureCollection>\n" );
		return outsb.toString();
	}

	public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
		return true;
	}
	/**
	 * returns null if there is an error.
	 *
	 * @param virtual_sensor_name
	 * @return
	 */
	public static ArrayList<StreamElement> getMostRecentValueFor(String virtual_sensor_name) {
		StringBuilder query=  new StringBuilder("select * from " ).append(virtual_sensor_name).append( " where timed = (select max(timed) from " ).append(virtual_sensor_name).append(")");
		ArrayList<StreamElement> toReturn=new ArrayList<StreamElement>() ;
		try {
			DataEnumerator result = StorageManager.getInstance( ).executeQuery( query , true );
			while ( result.hasMoreElements( ) )
				toReturn.add(result.nextElement());
		} catch (SQLException e) {
			logger.error("ERROR IN EXECUTING, query: "+query);
			logger.error(e.getMessage(),e);
			return null;
		}
		return toReturn;
	}
}
