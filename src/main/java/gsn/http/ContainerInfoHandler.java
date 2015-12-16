/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/http/ContainerInfoHandler.java
*
* @author Ali Salehi
* @author Behnaz Bostanipour
* @author Timotee Maret
* @author Ivo Dimitrov
* @author Milos Stojanovic
*
*/

package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.beans.WebInput;
import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import gsn.storage.DataEnumerator;
import org.apache.commons.collections.KeyValue;
import static org.apache.commons.lang.StringEscapeUtils.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;


public class ContainerInfoHandler implements RequestHandler {
  
  private static transient Logger logger             = LoggerFactory.getLogger( ContainerInfoHandler.class );
  
  public void handle ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
	  response.setStatus( HttpServletResponse.SC_OK );
	  String reqName = request.getParameter("name");

      //Added by Behnaz
      HttpSession session = request.getSession();
      User user = (User) session.getAttribute("user");

      response.setHeader("Cache-Control","no-store");
      response.setDateHeader("Expires", 0);
      response.setHeader("Pragma","no-cache");

      String omitLatestValuesPar = request.getParameter ( "omit_latest_values" );
      boolean omitLatestValues = false;
      if (omitLatestValuesPar != null && "true".equals(omitLatestValuesPar)){
          omitLatestValues = true;
      }

      response.getWriter( ).write( buildOutput(reqName,user, omitLatestValues));
  }
  
  //return only the requested sensor if specified (otherwise use null)
  public String buildOutput (String reqName, User user, boolean omitLatestValues) {
	  logger.trace("Start building reponse");
	  SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());	  
	  
      StringBuilder sb = new StringBuilder( "<gsn " );
      sb.append( "name=\"" ).append(escapeXml(Main.getContainerConfig().getWebName())).append( "\" " );
      sb.append( "author=\"" ).append(escapeXml(Main.getContainerConfig().getWebAuthor())).append( "\" " );
      sb.append( "email=\"" ).append(escapeXml(Main.getContainerConfig().getWebEmail())).append( "\" " );
      sb.append( "description=\"" ).append(escapeXml(Main.getContainerConfig().getWebDescription())).append("\">\n" );

      Iterator < VSensorConfig > vsIterator = Mappings.getAllVSensorConfigs( );

      boolean access;      // controls what will be shown from each sensor
      while ( vsIterator.hasNext( ) ) {
          access = true;  
          VSensorConfig sensorConfig = vsIterator.next( );
   
          logger.trace("check access for "+sensorConfig.getName());
          if(Main.getContainerConfig().isAcEnabled()){
        	  if (user != null){
                if ( (reqName != null && !sensorConfig.getName().equals(reqName) )|| 
                		( user.hasReadAccessRight(sensorConfig.getName())== false && user.isAdmin()==false) ) {
                	access = false;
                }
        	  }
        	  else {
                if ( (reqName != null && !sensorConfig.getName().equals(reqName)) || 
                		DataSource.isVSManaged(sensorConfig.getName())){
                  logger.trace("access protected");
                  access = false;
                }
        	  }
          }
          else {
            if ( (reqName != null && !sensorConfig.getName().equals(reqName))) continue;   
          }
          

          if (access == true) {
              logger.trace("access is granted");
	          sb.append("<virtual-sensor");
	          sb.append(" name=\"").append(sensorConfig.getName()).append("\"" );
	          sb.append(" protected=\"").append(" \"" );
	          sb.append(" last-modified=\"" ).append(new File(sensorConfig.getFileName()).lastModified()).append("\"");
	          if (sensorConfig.getDescription() != null) {
	              sb.append(" description=\"").append(escapeXml(sensorConfig.getDescription())).append("\"");
	          }
	          sb.append( ">\n" );
          ArrayList<StreamElement> ses = null;
          if (omitLatestValues == false) ses = getMostRecentValueFor(sensorConfig.getName());
          if (ses!=null ) {
              logger.trace("Adding fields and latest values ");
        	  for (StreamElement se:ses){
                  SimpleDateFormat fsdf = sensorConfig.getSDF() != null ? sensorConfig.getSDF() : sdf ;
                  sb.append("\t<field name=\"time\" type=\"string\" description=\"The timestamp associated with the stream element\" unit=\"\">" ).append( se == null ? "" : fsdf.format(new Date(se.getTimeStamp( ))) ).append( "</field>\n" );
                  for ( DataField df : sensorConfig.getOutputStructure( ) ) {
                      sb.append("\t<field");
                      sb.append(" name=\"").append(df.getName().toLowerCase()).append("\"");
                      sb.append(" type=\"").append(df.getType()).append("\"");
                      if (df.getDescription() != null && df.getDescription().trim().length() != 0)
                          sb.append(" description=\"").append(escapeXml(df.getDescription())).append("\"");

                      if (df.getUnit() != null && df.getUnit().trim().length() != 0)
                          sb.append(" unit=\"").append(df.getUnit()).append("\"");
                      else
                          sb.append(" unit=\"").append("").append("\"");
                      sb.append(">");
                      if (se!= null )
                          if (df.getType().toLowerCase( ).trim( ).indexOf( "binary" ) > 0 )
                              sb.append( se.getData( df.getName( ) ) );
                          else
                              sb.append( se.getData( escapeXml( df.getName( ) ) ) );
                      sb.append("</field>\n");
                  }
                  for ( KeyValue df : sensorConfig.getAddressing( )){
                      sb.append("\t<field");
                      sb.append(" name=\"").append( escapeXml( df.getKey( ).toString( ).toLowerCase()) ).append( "\"");
                      sb.append(" category=\"predicate\">");
                      sb.append(escapeXml( df.getValue( ).toString( ) ) );
                      sb.append("</field>\n" );
                  }
                  if (sensorConfig.getWebinput( )!=null){
                      for ( WebInput wi : sensorConfig.getWebinput( ) ) {
                          for ( DataField df : wi.getParameters ( ) ) {
                              sb.append( "\t<field");
                              sb.append(" command=\"").append( wi.getName( ) ).append( "\"" );
                              sb.append(" name=\"" ).append( df.getName( ).toLowerCase()).append( "\"" );
                              sb.append(" category=\"input\"");
                              sb.append(" type=\"").append( df.getType( ) ).append( "\"" );
                              if ( df.getDescription( ) != null && df.getDescription( ).trim( ).length( ) != 0 )
                                  sb.append( " description=\"" ).append( escapeXml( df.getDescription( ) ) ).append( "\"" );

                              if ( df.getUnit( ) != null && df.getUnit( ).trim( ).length( ) != 0 )
                                  sb.append( " unit=\"" ).append( df.getUnit( ) ).append( "\"" );
                              else
                                  sb.append( " unit=\"" ).append("").append( "\"" );
                              sb.append( "></field>\n" );
                          }
                      }
                  }
              }
          } else {
              logger.trace("Adding fields metadata");

              sb.append("\t<field name=\"time\" type=\"string\" description=\"The timestamp associated with the stream element\" unit=\"\">" ).append(  ""  ).append( "</field>\n" );
              for ( DataField df : sensorConfig.getOutputStructure( ) ) {
                  sb.append("\t<field");
                  sb.append(" name=\"").append(df.getName().toLowerCase()).append("\"");
                  sb.append(" type=\"").append(df.getType()).append("\"");
                  if (df.getDescription() != null && df.getDescription().trim().length() != 0)
                      sb.append(" description=\"").append(escapeXml(df.getDescription())).append("\"");

                  if (df.getUnit() != null && df.getUnit().trim().length() != 0)
                      sb.append(" unit=\"").append(df.getUnit()).append("\"");
                  else
                      sb.append(" unit=\"").append("").append("\"");
                  sb.append(">");
                  sb.append("</field>\n");
              }
              for ( KeyValue df : sensorConfig.getAddressing( )){
                  sb.append("\t<field");
                  sb.append(" name=\"").append( escapeXml( df.getKey( ).toString( ).toLowerCase()) ).append( "\"");
                  sb.append(" category=\"predicate\">");
                  sb.append(escapeXml( df.getValue( ).toString( ) ) );
                  sb.append("</field>\n" );
              }
              if (sensorConfig.getWebinput( )!=null){
                  for ( WebInput wi : sensorConfig.getWebinput( ) ) {
                      for ( DataField df : wi.getParameters ( ) ) {
                          sb.append( "\t<field");
                          sb.append(" command=\"").append( wi.getName( ) ).append( "\"" );
                          sb.append(" name=\"" ).append( df.getName( ).toLowerCase()).append( "\"" );
                          sb.append(" category=\"input\"");
                          sb.append(" type=\"").append( df.getType( ) ).append( "\"" );
                          if ( df.getDescription( ) != null && df.getDescription( ).trim( ).length( ) != 0 )
                              sb.append( " description=\"" ).append( escapeXml( df.getDescription( ) ) ).append( "\"" );

                          if ( df.getUnit( ) != null && df.getUnit( ).trim( ).length( ) != 0 )
                              sb.append( " unit=\"" ).append( df.getUnit( ) ).append( "\"" );
                          else
                              sb.append( " unit=\"" ).append("").append( "\"" );
                          sb.append( "></field>\n" );
                      }
                  }
              }
          }
      } else {
          logger.trace("Adding sensor minimal description ");

          sb.append("<virtual-sensor");
          sb.append(" name=\"").append(sensorConfig.getName()).append("\"" );  // (protected)
          sb.append(" protected=\"").append(" (protected)\"" );
          sb.append(" last-modified=\"" ).append(new File(sensorConfig.getFileName()).lastModified()).append("\"");
          StringBuffer fields = new StringBuffer();           // gather information about the field names and types
          fields.append("Fields:@ ");
          for ( DataField df : sensorConfig.getOutputStructure( ) ) {
               fields.append("name = ").append(df.getName().toLowerCase()).append(", ");
               String unit = df.getUnit();
               if (unit == null) unit = "";
               fields.append("unit = ").append(unit.toLowerCase()).append(", ");
               fields.append("type = ").append(df.getType()).append(" | ");
          }

          StringBuffer location = new StringBuffer();         // gather information about the location of the sensor
          location.append("Location:@ ");
          for ( KeyValue df : sensorConfig.getAddressing( )){
              location.append(escapeXml( df.getKey( ).toString( ).toLowerCase())+" = "+escapeXml( df.getValue( ).toString( ) )+" | " );
          }
          location.append("\n");
          //System.out.println("Loc = "+location.toString() );
          if (sensorConfig.getDescription() != null) {           // added May 2013
              sb.append(" description=\"").append("Information:@"+escapeXml(sensorConfig.getDescription())).append(" # ").append(location.toString()).append(" # ").append(fields.toString()).append("\"");     // .append(location.toString()+"").append(fields.toString())
              // sb.append(" description=\"").append("&lt;dl&gt; &lt;dt&gt;  INFORMATION: &lt;/dt&gt &lt;dd&gt;").append(StringEscapeUtils.escapeXml(sensorConfig.getDescription())).append(" &lt;/dd&gt &lt;/dl&gt;").append("\"");     // .append(location.toString()+"").append(fields.toString())
              //           sb.append(" description=\"").append("&lt;div&gt;  &lt;p&gt; INFORMATION: ").append(StringEscapeUtils.escapeXml(sensorConfig.getDescription())).append(" &lt;/p&gt; &lt;/div&gt;").append("\"");     // .append(location.toString()+"").append(fields.toString())
          }
          sb.append( ">\n" );
      }

      sb.append( "</virtual-sensor>\n" );
    }
    sb.append( "</gsn>\n" );
      //System.out.println(sb.toString());
    return sb.toString();
  }
  
  public boolean isValid ( HttpServletRequest request , HttpServletResponse response ) throws IOException {
    return true;
  }
  /**
   * returns null if there is an error.
   * only return the latest value, in case of non-unique time-stamps, the primary key is used.
   * @param virtual_sensor_name
   * @return
   */
  public static ArrayList<StreamElement> getMostRecentValueFor(String virtual_sensor_name) {
    //System.out.println("GET NEW FOR = "+virtual_sensor_name);
    StringBuilder query=  new StringBuilder("select * from " ).append(virtual_sensor_name).append( " where timed = (select max(timed) from " ).append(virtual_sensor_name).append(") order by pk desc limit 1");
    ArrayList<StreamElement> toReturn=new ArrayList<StreamElement>() ;
    try {
      DataEnumerator result = Main.getStorage(virtual_sensor_name).executeQuery( query , true );
      while ( result.hasMoreElements( ) ) 
        toReturn.add(result.nextElement());
    } catch (SQLException e) {
      logger.error("ERROR IN EXECUTING, query: "+query+": "+e.getMessage());
      return null;
    }
    return toReturn;
  }
}
