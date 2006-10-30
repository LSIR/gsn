package gsn.web;

import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.VSensorConfig;
import gsn.storage.StorageManager;

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
      String primaryKey = req.getParameter( "pk" ).trim( );
      String colName = req.getParameter( "field" ).trim( );
      if ( primaryKey == null || colName == null || vsName == null ) return;
      
      StringBuilder query = new StringBuilder( ).append( prefix ).append( vsName ).append( postfix );
      VSensorConfig sensorConfig = Mappings.getVSensorConfig(vsName);
      if (sensorConfig == null) {
			// TODO : ERROR "Requested virtual sensor doesn't exist >"+ prespectiveVirtualSensor + "<."
			return;
		}
      ResultSet rs = StorageManager.getInstance( ).getBinaryFieldByQuery( query , colName , Long.parseLong( primaryKey ) );
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
