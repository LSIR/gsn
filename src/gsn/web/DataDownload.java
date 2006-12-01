package gsn.web;

import gsn.Container;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

/**
 * @author Pierre-Alain Robert (PARO, pierre-alain.robert-at-epfl.ch)<br>
 * Create : 2006 <br>
 * Created for : GSN project. <br>
 * @web.servlet name="DataDownload" load-on-startup="true"
 * @web.servlet-mapping url-pattern="/field"
 */

public class DataDownload extends HttpServlet {
	
	public void  doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		doPost(req, res);
	}
	
	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
	      boolean responseCVS = false;
	      boolean wantTimeStamp = false;
	      boolean commonReq = true;
		  PrintWriter out = res.getWriter();
	      if (req.getParameter("vsName")==null || req.getParameter("vsName").equals("")) {
	    	  res.sendError( Container.MISSING_VSNAME_ERROR , "The virtual sensor name is missing" );
	    	  return;
	      }
	      if (req.getParameter("display") != null && req.getParameter("display").equals("CSV")) {
	    	  responseCVS = true;
		      res.setContentType("text/csv");
	    	  //res.setContentType("text/html");
	      } else {
	    	  res.setContentType("text/xml");
	      }
	      if (req.getParameter("commonReq") != null && req.getParameter("commonReq").equals("false")) {
	    	  commonReq = false;
	      }
	      String vsName = req.getParameter("vsName");
	      String delimiter = ";";
	      if (req.getParameter("delimiter") != null && !req.getParameter("delimiter").equals("")) {
	    	  String reqdelimiter = req.getParameter("delimiter");
	    	  if (reqdelimiter.equals("tab")) {
	    		  delimiter = "\t";
	    	  } else if (reqdelimiter.equals("space")){
	    		  delimiter = " ";
	    	  } else if (reqdelimiter.equals("other") && req.getParameter("otherdelimiter") != null && !req.getParameter("otherdelimiter").equals("")) {
	    		  delimiter = req.getParameter("otherdelimiter");
	    	  }
	      }
	      String request = "";
	      String expression = "";
	      String line="";
	      if (commonReq) {
		      if (req.getParameter("fields") != null) {
		    	  String[] fields = req.getParameterValues("fields");
			      for (int i=0; i < fields.length; i++) {
			    	  if (fields[i].equals("TIMED")) {
			    		  wantTimeStamp = true;
			    	  }
			    	  request += ", " + fields[i];
			      }    
		      }
	      } else {
	    	  String field;
	    	  if (req.getParameter("fields") == null) {
	    		  out.println("Request ERROR");
	    		  return;
	    	  } else {
	    		  field = req.getParameter("fields");
	    	  }
		      String aggregateFunction = "AVG";
		      if (req.getParameter("aggregateFunction")!= null) {
		    	  if (req.getParameter("aggregateFunction").equals("MIN")) {
		    		  aggregateFunction = "MIN";
		    	  } else if (req.getParameter("aggregateFunction").equals("MAX")) {
		    		  aggregateFunction = "MAX";
		    	  }
		      }
		      request += "  " + aggregateFunction + "(" + field + ") ";
	      }
	      
	      String limit = "";
	      	      if (req.getParameter("nb") != null && req.getParameter("nb") != "") {
	    	  int nb = new Integer(req.getParameter("nb"));
	    	  if (nb > 0) {
	    		  limit = "LIMIT " + nb + "  offset 0";
	    	  }
	      }
	      String where = "";
	      if (req.getParameter("critfield") != null) {
	    	  try {
		    	  String[] critJoin = req.getParameterValues("critJoin");
		    	  String[] neg = req.getParameterValues("neg");
		    	  String[] critfields = req.getParameterValues("critfield");
		    	  String[] critop = req.getParameterValues("critop");
		    	  String[] critval = req.getParameterValues("critval");
		    	  for (int i=0; i < critfields.length ; i++) {
		    		  if (critop[i].equals("LIKE")) {
		    			  if (i > 0) {
		    				  where += " " + critJoin[i-1] + " " + neg[i] + " " + critfields[i] + " LIKE '%"; // + critval[i] + "%'";
		    			  } else {
		    				  where += neg[i] + " " + critfields[i] + " LIKE '%"; // + critval[i] + "%'";
		    			  }
		    			  if (critfields[i].equals("TIMED")) {
		    				  try {
		    					  SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
		    					  Date d = sdf.parse(critval[i]);
			    				  where += d.getTime();
		    				  } catch (Exception e) {
		    					  where += "0";
		    				  }
		    			  } else {
		    				  where += critval[i];
		    			  }
		    			  where += "%'";
		    		  } else {
		    			  if (i > 0) {
		    				  where += " " + critJoin[i-1] + " " + neg[i] + " " + critfields[i] + " " + critop[i] + " "; //critval[i];
		    			  } else {
		    				  where += neg[i] + " " + critfields[i] + " " + critop[i] + " "; //critval[i];
		    			  }
		    			  if (critfields[i].equals("TIMED")) {
		    				  try {
		    					  SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
		    					  Date d = sdf.parse(critval[i]);
		    					  where += d.getTime();
		    				  } catch (Exception e) {
		    					  System.out.println(e.toString());
		    					  where += "0";
		    				  }
		    			  } else {
		    				  where += critval[i];
		    			  }
		    		  }
		    	  }
		    	  where = " WHERE " + where;
	    	  } catch (NullPointerException npe) {
	    		  where = " ";
	    	  }
	      }
	      	      
	      if (! request.equals("")) {
	    	  request = request.substring(2);
	    	  if (!commonReq) {
	    		  expression = request;
	    	  }
	    	  request = "select "+request+" from " + vsName + where;
	    	  if (commonReq) {
	    		  request += " order by TIMED DESC "+limit;
	    	  }
	    	  request += ";";
	    	  StringBuilder query = new StringBuilder(request);
	    	  //out.println(query);
	    	  ///*
	    	  DataEnumerator  result = StorageManager.getInstance( ).executeQuery( query , false );
	    	  if (result.IsNull()) {
	    		  res.setContentType("text/html");
	    		  out.println("No data corresponds to your request");
	    		  return;
	    	  }
	    	  line = "";
	    	  if (responseCVS) {
	    		  boolean firstLine = true;
	    		  out.println(query);
		          while ( result.hasMoreElements( ) ) {
		             StreamElement se = result.nextElement( );
		             if (firstLine) {
		            	 for (int i=0; i < se.getFieldNames().length; i++)
		            		 line += delimiter + se.getFieldNames()[i].toString();
		            	if (wantTimeStamp) {
		            		line += delimiter + "TIMED";
		            	}
		            	firstLine = false;
		             }
		             out.println(line.substring(delimiter.length()));
		             line = "";
		             for ( int i = 0 ; i < se.getFieldNames( ).length ; i++ )
		                   line += delimiter+se.getData( )[ i ].toString( );
		             if (wantTimeStamp) {
		            	SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
					 	Date d = new Date (se.getTimeStamp());
					 	line += delimiter + sdf.format(d);
		             }
		             out.println(line.substring(delimiter.length()));
		          }
	    	  } else {
	    		  boolean firstLine = true;
	    		  out.println("<data>");
	    		  while ( result.hasMoreElements( ) ) {
		             StreamElement se = result.nextElement( );
	    			  if (firstLine) {
	    				 out.println("\t<line>");
	    				 for (int i = 0; i < se.getFieldNames().length; i++)
	    					 if (commonReq) {
		    					 out.println("\t\t<field>" + se.getFieldNames()[i].toString()+"</field>");
	    					 } else {
	    						 out.println("\t\t<field>"+expression+"</field>");
	    					 }
	    				 if (wantTimeStamp) {
	    					 out.println("\t\t<field>TIMED</field>");
	    				 }
	    				 out.println("\t</line>");
	    				 firstLine = false;
	    			  }
		             line = "";
		             out.println("\t<line>");
		             for ( int i = 0 ; i < se.getFieldNames( ).length ; i++ )
		                   out.println("\t\t<field>"+se.getData( )[ i ].toString( )+"</field>");
		             if (wantTimeStamp) {
		            	SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss");
					 	Date d = new Date (se.getTimeStamp());
		            	out.println("\t\t<field>"+sdf.format(d)+"</field>");
		             }
		             out.println("\t</line>");
			       }
	    		  out.println("</data>");
	    	  }
	    	result.close();
	    	//*/
	      } else {
	    	  out.println("Please select some fields");
	      }
	   }
}