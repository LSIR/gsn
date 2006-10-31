package gsn.web;

import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;
import gsn.vsensor.Container;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



//path="/WEB-INF/file_not_found.jpg"
/**
 * @author Ali Salehi (AliS, ali.salehi-at-epfl.ch)<br>
 * Create : 2006 <br>
 * Created for : GSN project. <br>
 * @web.servlet name="BinaryDownload" load-on-startup="true"
 * @web.servlet-mapping url-pattern="/field"
 */
public class FieldDownloadServlet extends HttpServlet {
   
   static final String prefix  = "select * from ";
   
   static final String postfix = " where PK = ? ";
   
   public void doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
      String vsName = req.getParameter( "vs" );
      if (vsName==null ||vsName.trim().length()==0) {
			res.sendError(Container.MISSING_VSNAME_ERROR , "The virtual sensor name is missing");
			return;
      }
      String primaryKey = req.getParameter( "pk" );
      String colName = req.getParameter( "field" );
      if ( primaryKey == null || colName == null ||primaryKey.trim().length()==0 || colName.trim().length()==0  ) {
    	  res.sendError(res.SC_BAD_REQUEST , "The pk and/or field parameters are missing.");
		  return;
      }
      VSensorConfig sensorConfig = Mappings.getVSensorConfig(vsName.trim());
      if (sensorConfig == null) {
			res.sendError(Container.ERROR_INVALID_VSNAME , "The specified virtual sensor doesn't exist.");
			return;
	  }
	
      primaryKey=primaryKey.trim( );
      colName=colName.trim( );
      
      StringBuilder query = new StringBuilder( ).append( prefix ).append( vsName ).append( postfix );
      ResultSet rs = StorageManager.getInstance( ).getBinaryFieldByQuery( query , colName , Long.parseLong( primaryKey ) );
      if (rs==null) {
    	  res.sendError(res.SC_NOT_FOUND, "The requested data is marked as obsolete and is not available.");
    	  return;  
      }
      res.setContentType("text/xml");
      res.getWriter().println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
	  boolean binary = false;
      for (DataField df : sensorConfig.getOutputStructure()) 
			if (df.getFieldName().toLowerCase().equals(colName.trim().toLowerCase()))
				if (df.getDataTypeID()==DataTypes.BINARY) {
					StringTokenizer st = new StringTokenizer(df.getType(),":");
					if (st.countTokens()!=2)
						return;
					st.nextToken();//Ignoring the first token.
					res.setContentType(st.nextToken());
					binary=true;
				}
      try {
      if (binary)
    	  res.getOutputStream( ).write( rs.getBytes(colName) );  
      else
    	  res.getWriter().write(rs.getString(colName));
      }catch (Exception e) {
    	e.printStackTrace();
	  }finally {
		  if (rs!=null)
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
	  }
      //if ( type.equalsIgnoreCase( "svg" ) ) res.setContentType( "" );   
   }
   
   public void doPost ( HttpServletRequest request , HttpServletResponse response ) throws ServletException , IOException {
      doGet( request , response );
   }
   
}
