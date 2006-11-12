package gsn.web;

import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import gsn.vsensor.Container;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	      PrintWriter out = res.getWriter();
	      if (req.getParameter("vsName")==null || req.getParameter("vsName").equals("")) {
	    	  res.sendError( Container.MISSING_VSNAME_ERROR , "The virtual sensor name is missing" );
	    	  return;
	      }
	      res.setContentType("text/csv");
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
	      String line="";
	      if (req.getParameter("fields") != null) {
	    	  String[] fields = req.getParameterValues("fields");
		      for (int i=0; i < fields.length; i++) {
		    	  request += ", " + fields[i];
		    	  line += delimiter + fields[i];
		      }    
	      }
	      out.println(line.substring(delimiter.length()));
	      
	      String limit = "";
	      	      if (req.getParameter("nb") != null && req.getParameter("nb") != "") {
	    	  int nb = new Integer(req.getParameter("nb"));
	    	  if (nb > 0) {
	    		  limit = "LIMIT " + nb + "  offset 0";
	    	  }
	      }
	      String where = "";
	      if (req.getParameter("critfield") != null) {
	    	  String[] critfields = req.getParameterValues("critfield");
	    	  String[] critop = req.getParameterValues("critop");
	    	  String[] critval = req.getParameterValues("critval");
	    	  for (int i=0; i < critfields.length ; i++) {
	    		  if (critop[i].equals("LIKE")) {
	    			  where += " AND " + critfields[i] + " LIKE '%" + critval[i] + "%'";
	    		  } else {
	    			  where += " AND " + critfields[i] + " " + critop[i] + " " + critval[i];
	    		  }
	    	  }
	    	  where = where.substring(4);
	    	  where = " WHERE " + where;
	      }
	      	      
	      if (! request.equals("")) {
	    	  request = request.substring(2);
	    	  StringBuilder query = new StringBuilder("select "+request+" from " + vsName + where + " order by TIMED DESC "+limit+";");
	    	  DataEnumerator  result = StorageManager.getInstance( ).executeQuery( query , false );
	          while ( result.hasMoreElements( ) ) {
	             StreamElement se = result.nextElement( );
	             line = "";
	             for ( int i = 0 ; i < se.getFieldNames( ).length ; i++ )
	                   line += delimiter+se.getData( )[ i ].toString( );
	             out.println(line.substring(delimiter.length()));
	          }
	          result.close();
	      } else {
	    	  out.println("Please select some fields");
	      }
	   }
}