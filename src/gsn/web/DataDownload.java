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
	      if (req.getParameterValues("fields") == null || req.getParameter("vsName") == null 
	    		|| req.getParameter("delimiter") == null || req.getParameter("nb") == null ) {
	    	  out.println("Missing request parameter");
	    	  return;
	      }
	      res.setContentType("text/csv");
	      String[] fields = req.getParameterValues("fields");
	      String vsName = req.getParameter("vsName");
	      String delimiter = req.getParameter("delimiter");
	      if(delimiter.equals("")) {
	    	  delimiter = ";";
	      }
	      String request = "";
	      int nb = new Integer(req.getParameter("nb"));
	      String limit;
	      String line="";
	      if (nb == 0) {
	    	  limit = "";
	      } else {
	    	  limit = "LIMIT " + nb + " ";
	      }
	      for (int i=0; i < fields.length; i++) {
	    	  request += ", " + fields[i];
	    	  line += delimiter + fields[i];
	      }
	      out.println(line.substring(delimiter.length()));
	      if (! request.equals("")) {
	    	  request = request.substring(2);
	    	  StringBuilder query = new StringBuilder("select "+request+" from " + vsName + " order by TIMED "+limit+" offset 0");
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