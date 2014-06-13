/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

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

//import gsn.http.accesscontrol.User;


//import gsn.http.accesscontrol.User;

public class ContainerInfoHandler implements RequestHandler {
  
  private static transient Logger logger             = Logger.getLogger( ContainerInfoHandler.class );
  
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

  //System.out.println( "The handle was called" );
 // if (reqName != null) System.out.println("requst " + reqName);
 // if (user != null) System.out.println("User " + user.getUserName());
        response.getWriter( ).write( buildOutput(reqName,user, omitLatestValues));
  }

  
  //return only the requested sensor if specified (otherwise use null)
  //Added by Behnaz. New parameter User user to method buildOutput.
  public String buildOutput (String reqName, User user, boolean omitLatestValues) {
	  SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());
	  
	  
    StringBuilder sb = new StringBuilder( "<gsn " );
    sb.append( "name=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebName( ) ) ).append( "\" " );
    sb.append( "author=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebAuthor( ) ) ).append( "\" " );
    sb.append( "email=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebEmail( ) ) ).append( "\" " );
    sb.append( "description=\"" ).append( StringEscapeUtils.escapeXml( Main.getContainerConfig( ).getWebDescription( ) ) ).append("\">\n" );


    Iterator < VSensorConfig > vsIterator = Mappings.getAllVSensorConfigs( );

    boolean access;      // controls what will be shown from each sensor
    while ( vsIterator.hasNext( ) ) {
      access = true;  // by default it is considered that everything from the sensor will be retrieved
      VSensorConfig sensorConfig = vsIterator.next( );
   //System.out.println( "Inside buildOutput -- name = "+sensorConfig.getName());
      if(Main.getContainerConfig().isAcEnabled())
      {
          if (user != null)
          {
              //System.out.println( "The user is defined"+sensorConfig.getName());
              if ( (reqName != null && !sensorConfig.getName().equals(reqName) )|| ( user.hasReadAccessRight(sensorConfig.getName())== false && user.isAdmin()==false) ) {//continue;
                                   access = false;
                  //System.out.println("Source = "+sensorConfig.getName()+" has access = "+ access);
              }
          }
          else {
              //System.out.println("Datasource - "+DataSource.isVSManaged(sensorConfig.getName()));
              if ( (reqName != null && !sensorConfig.getName().equals(reqName)) || DataSource.isVSManaged(sensorConfig.getName()))
              {
                  access = false;
                  // continue;
              }
          }
      }
      else
      {
            if ( (reqName != null && !sensorConfig.getName().equals(reqName))) continue;   
      }

      if (access == true) {
          sb.append("<virtual-sensor");
          sb.append(" name=\"").append(sensorConfig.getName()).append("\"" );
          sb.append(" protected=\"").append(" \"" );
          sb.append(" last-modified=\"" ).append(new File(sensorConfig.getFileName()).lastModified()).append("\"");
          if (sensorConfig.getDescription() != null) {
              sb.append(" description=\"").append(StringEscapeUtils.escapeXml(sensorConfig.getDescription())).append("\"");
          }
          sb.append( ">\n" );
          ArrayList<StreamElement> ses = null;
          if (omitLatestValues == false) ses = getMostRecentValueFor(sensorConfig.getName());
          int counter = 1;
          if (ses!=null ) {
              for (StreamElement se:ses){
                  SimpleDateFormat fsdf = sensorConfig.getSDF() != null ? sensorConfig.getSDF() : sdf ;
                  sb.append("\t<field name=\"time\" type=\"string\" description=\"The timestamp associated with the stream element\" unit=\"\">" ).append( se == null ? "" : fsdf.format(new Date(se.getTimeStamp( ))) ).append( "</field>\n" );
                  for ( DataField df : sensorConfig.getOutputStructure( ) ) {
                      sb.append("\t<field");
                      sb.append(" name=\"").append(df.getName().toLowerCase()).append("\"");
                      sb.append(" type=\"").append(df.getType()).append("\"");
                      if (df.getDescription() != null && df.getDescription().trim().length() != 0)
                          sb.append(" description=\"").append(StringEscapeUtils.escapeXml(df.getDescription())).append("\"");

                      if (df.getUnit() != null && df.getUnit().trim().length() != 0)
                          sb.append(" unit=\"").append(df.getUnit()).append("\"");
                      else
                          sb.append(" unit=\"").append("").append("\"");
                      sb.append(">");
                      if (se!= null )
                          if (df.getType().toLowerCase( ).trim( ).indexOf( "binary" ) > 0 )
                              sb.append( se.getData( df.getName( ) ) );
                          else
                              sb.append( se.getData( StringEscapeUtils.escapeXml( df.getName( ) ) ) );
                      sb.append("</field>\n");
                  }
                  for ( KeyValue df : sensorConfig.getAddressing( )){
                      sb.append("\t<field");
                      sb.append(" name=\"").append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase()) ).append( "\"");
                      sb.append(" category=\"predicate\">");
                      sb.append(StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ) );
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
                                  sb.append( " description=\"" ).append( StringEscapeUtils.escapeXml( df.getDescription( ) ) ).append( "\"" );

                              if ( df.getUnit( ) != null && df.getUnit( ).trim( ).length( ) != 0 )
                                  sb.append( " unit=\"" ).append( df.getUnit( ) ).append( "\"" );
                              else
                                  sb.append( " unit=\"" ).append("").append( "\"" );
                              sb.append( "></field>\n" );
                          }
                      }
                  }
                  counter++;
              }
          } else {
              SimpleDateFormat fsdf = sensorConfig.getSDF() != null ? sensorConfig.getSDF() : sdf ;
              sb.append("\t<field name=\"time\" type=\"string\" description=\"The timestamp associated with the stream element\" unit=\"\">" ).append(  ""  ).append( "</field>\n" );
              for ( DataField df : sensorConfig.getOutputStructure( ) ) {
                  sb.append("\t<field");
                  sb.append(" name=\"").append(df.getName().toLowerCase()).append("\"");
                  sb.append(" type=\"").append(df.getType()).append("\"");
                  if (df.getDescription() != null && df.getDescription().trim().length() != 0)
                      sb.append(" description=\"").append(StringEscapeUtils.escapeXml(df.getDescription())).append("\"");

                  if (df.getUnit() != null && df.getUnit().trim().length() != 0)
                      sb.append(" unit=\"").append(df.getUnit()).append("\"");
                  else
                      sb.append(" unit=\"").append("").append("\"");
                  sb.append(">");
                  sb.append("</field>\n");
              }
              for ( KeyValue df : sensorConfig.getAddressing( )){
                  sb.append("\t<field");
                  sb.append(" name=\"").append( StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase()) ).append( "\"");
                  sb.append(" category=\"predicate\">");
                  sb.append(StringEscapeUtils.escapeXml( df.getValue( ).toString( ) ) );
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
                              sb.append( " description=\"" ).append( StringEscapeUtils.escapeXml( df.getDescription( ) ) ).append( "\"" );

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
              location.append(StringEscapeUtils.escapeXml( df.getKey( ).toString( ).toLowerCase())+" = "+StringEscapeUtils.escapeXml( df.getValue( ).toString( ) )+" | " );
          }
          location.append("\n");
          //System.out.println("Loc = "+location.toString() );
          if (sensorConfig.getDescription() != null) {           // added May 2013
              sb.append(" description=\"").append("Information:@"+StringEscapeUtils.escapeXml(sensorConfig.getDescription())).append(" # ").append(location.toString()).append(" # ").append(fields.toString()).append("\"");     // .append(location.toString()+"").append(fields.toString())
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
   * 
   * @param virtual_sensor_name
   * @return
   */
  public static ArrayList<StreamElement> getMostRecentValueFor(String virtual_sensor_name) {
    //System.out.println("GET NEW FOR = "+virtual_sensor_name);
    StringBuilder query=  new StringBuilder("select * from " ).append(virtual_sensor_name).append( " where timed = (select max(timed) from " ).append(virtual_sensor_name).append(")");
    ArrayList<StreamElement> toReturn=new ArrayList<StreamElement>() ;
    try {
      DataEnumerator result = Main.getStorage(virtual_sensor_name).executeQuery( query , true );
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
