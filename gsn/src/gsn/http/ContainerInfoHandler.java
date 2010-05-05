package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.beans.WebInput;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.KeyValue;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

public class ContainerInfoHandler implements RequestHandler {
  
  private static transient Logger logger             = Logger.getLogger( ContainerInfoHandler.class );
  
  public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
	  response.setStatus( HttpServletResponse.SC_OK );
	  String reqName = request.getParameter("name");

    response.getWriter( ).write( buildOutput(reqName) );
  }
  
  //return only the requested sensor if specified (otherwise use null)
  public String buildOutput (String reqName) {
	  SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());
	  
	  
    StringBuilder sb = new StringBuilder( "<gsn " );
    sb.append( "name=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebName( ) ) ).append( "\" " );
    sb.append( "author=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebAuthor( ) ) ).append( "\" " );
    sb.append( "email=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebEmail( ) ) ).append( "\" " );
    sb.append( "description=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebDescription( ) ) ).append("\">\n" );
    
    Iterator < VSensorConfig > vsIterator = Mappings.getAllVSensorConfigs( );
    
    
    while ( vsIterator.hasNext( ) ) {
      VSensorConfig sensorConfig = vsIterator.next( );
      if ( reqName != null && !sensorConfig.getName().equals(reqName) ) continue;
      sb.append("<virtual-sensor");
      sb.append(" name=\"").append(sensorConfig.getName()).append("\"" );
      sb.append(" last-modified=\"" ).append(new File(sensorConfig.getFileName()).lastModified()).append("\"");
      if (sensorConfig.getDescription() != null) {
        sb.append(" description=\"").append(StringEscapeUtils.escapeXml(sensorConfig.getDescription())).append("\"");
      }
      sb.append( ">\n" );
      
      ArrayList<StreamElement> ses = getMostRecentValueFor(sensorConfig.getName());
      int counter = 1;
      if (ses!=null ) {
        for (StreamElement se:ses){
          for ( DataField df : sensorConfig.getOutputStructure( ) ) {
          	boolean unixtime=false;
        	boolean relatime=false;
            sb.append("\t<field");
            sb.append(" name=\"").append(df.getName().toLowerCase()).append("\"");
            sb.append(" type=\"").append(df.getType()).append("\"");
            if (df.getUnit() != null && df.getUnit().trim().length() != 0) {
            	if(df.getUnit().compareToIgnoreCase("unixtime")==0) {
            		unixtime = true;
            	}
            	else if(df.getUnit().compareToIgnoreCase("relatime")==0) {
            		relatime = true;
            	}
            	else
                	sb.append(" unit=\"").append(StringEscapeUtils.escapeXml(df.getUnit())).append("\"");
            }
            if (df.getDescription() != null && df.getDescription().trim().length() != 0)
              sb.append(" description=\"").append(StringEscapeUtils.escapeXml(df.getDescription())).append("\"");
            sb.append(">");
            if (se!= null ) 
              if (df.getType().toLowerCase( ).trim( ).indexOf( "binary" ) > 0 )
                sb.append( se.getData( df.getName( ) ) );
              else if (unixtime) {
            	  try {
            		  Long t = (Long)se.getData(StringEscapeUtils.escapeXml( df.getName( ) ));
            		  if(t != null)
            			  sb.append(sdf.format(new Date(t)));
            		  else
            			  sb.append(t);
            	  }
            	  catch (ClassCastException e) {
            		  logger.warn("Stream element ["+se+"] could not be cast to date string");
            		  sb.append("null");
            	  }
              }
              else if (relatime) {
            	  try {
            		  Integer seconds = (Integer)se.getData(StringEscapeUtils.escapeXml( df.getName( ) ));
            		  if(seconds != null) {
            			  Integer minutes = (Integer) ((seconds / 60) % 60);
            			  Integer hours = (Integer) ((seconds / 3600) % 24);
            			  Integer days = (Integer) (seconds / 86400);

            			  if(days == 0)
            				  if(hours == 0)
            					  if(minutes == 0)
                        			  sb.append(String.format("%ds", (Integer)(seconds % 60)));
            					  else
            						  sb.append(String.format("%dm %ds", minutes,  (Integer)(seconds % 60)));
            				  else
            					  sb.append(String.format("%dh %dm %ds", hours, minutes,  (Integer)(seconds % 60)));
            			  else
            				  sb.append(String.format("%dd %dh %dm %ds", days, hours, minutes,  (Integer)(seconds % 60)));
            		  }
            		  else
            			  sb.append(seconds);
            	  }
            	  catch (ClassCastException e) {
            		  logger.warn("Stream element ["+se+"] could not be cast to date string");
            		  sb.append("null");
            	  }
              }
              else
                sb.append( se.getData( StringEscapeUtils.escapeXml( df.getName( ) ) ) );
            sb.append("</field>\n");
          }
          sb.append("\t<field name=\"timed\" type=\"string\" description=\"The timestamp associated with the stream element\">" ).append( se == null ? "" : sdf.format(new Date(se.getTimeStamp( ))) ).append( "</field>\n" );
          for ( KeyValue df : sensorConfig.getAddressing( )){
            sb.append("\t<field");
            sb.append(" name=\"").append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase()) ).append( "\"");
            sb.append(" category=\"predicate\">");
            sb.append(StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ) );
            sb.append("</field>\n" );
          }
          counter++;
        }
      }
      if (sensorConfig.getWebinput( )!=null){
          for ( WebInput wi : sensorConfig.getWebinput( ) ) {
            for ( DataField df : wi.getParameters ( ) ) {
              sb.append( "\t<field");
              sb.append(" command=\"").append( wi.getName( ) ).append( "\"" );
              sb.append(" name=\"" ).append( df.getName( ).toLowerCase()).append( "\"" );
              sb.append(" category=\"input\"");
              sb.append(" type=\"").append( df.getType( ) ).append( "\"" );
              if ( df.getDefaultValue() != null)
                  sb.append( " defaultvalue=\"" ).append( StringEscapeUtils.escapeXml( df.getDefaultValue()) ).append( "\"" );                
              if ( df.getDescription( ) != null && df.getDescription( ).trim( ).length( ) != 0 )
                sb.append( " description=\"" ).append( StringEscapeUtils.escapeXml( df.getDescription( ) ) ).append( "\"" );
              sb.append( "></field>\n" );
            }
          }
      }
      sb.append( "</virtual-sensor>\n" );
    }
    sb.append( "</gsn>\n" );
    return sb.toString();
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
