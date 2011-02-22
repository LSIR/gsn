package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.InputStream;
import gsn.beans.VSensorConfig;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import org.apache.log4j.Logger;

//path="/WEB-INF/file_not_found.jpg"

public class FieldDownloadServlet extends HttpServlet {

	private static transient Logger                                      logger                             = Logger.getLogger( FieldDownloadServlet.class );

	static final String prefix  = "select * from ";

	static final String postfix = " where PK = ? ";

	public void doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		String vsName = req.getParameter( "vs" );

        //
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");

        res.setHeader("Cache-Control","no-store");
        res.setDateHeader("Expires", 0);
        res.setHeader("Pragma","no-cache");

        if ( Main.getContainerConfig().isAcEnabled() && DataSource.isVSManaged(vsName)) {
            if(user == null || (! user.isAdmin() && ! user.hasReadAccessRight(vsName))) {
                res.sendError(WebConstants.ACCESS_DENIED, "Access Control failed for vsName:" + vsName + " and user: " + (user == null ? "not logged in" : user.getUserName()));
                return;
            }
        }
        //

        if ( vsName == null || (vsName =vsName.trim( ).toLowerCase()).length( ) == 0 ) {
			res.sendError( WebConstants.MISSING_VSNAME_ERROR , "The virtual sensor name is missing" );
			return;
		}
		String primaryKey = req.getParameter( "pk" );
		String colName = req.getParameter( "field" );
		if ( primaryKey == null || colName == null || primaryKey.trim( ).length( ) == 0 || colName.trim( ).length( ) == 0 ) {
			res.sendError( res.SC_BAD_REQUEST , "The pk and/or field parameters are missing." );
			return;
		}
		VSensorConfig sensorConfig = Mappings.getVSensorConfig( vsName );
		if ( sensorConfig == null ) {
			res.sendError( WebConstants.ERROR_INVALID_VSNAME , "The specified virtual sensor doesn't exist." );
			return;
		}

		primaryKey = primaryKey.trim( );
		colName = colName.trim( );
		StringBuilder query;
		Long pk = null;
		if (primaryKey.compareToIgnoreCase("latest")==0) {
			query = new StringBuilder( ).append( prefix ).append( vsName ).append(" where timed = (select max(timed) from " ).append(vsName).append(")");
		}
		else {
			pk = Long.parseLong(primaryKey);
			query = new StringBuilder( ).append( prefix ).append( vsName ).append( postfix );
		}
		// TODO : Check to see if the requested column exists.
		Connection conn = null;
		try {
			conn = Main.getStorage(vsName).getConnection();
			ResultSet rs = Main.getStorage(vsName).getBinaryFieldByQuery( query , colName , pk ,conn);
			if ( !rs.next() ) {
				res.sendError( res.SC_NOT_FOUND , "The requested data is marked as obsolete and is not available." );
			}else {
				boolean binary = false;
				String filename = null;
				for ( DataField df : sensorConfig.getOutputStructure( ) )
					if ( df.getName( ).toLowerCase( ).equals( colName.trim( ).toLowerCase( ) ) ) {
						if ( df.getDataTypeID( ) == DataTypes.BINARY ) {
							StringTokenizer st = new StringTokenizer( df.getType( ) , ":" );
							binary = true;
							if ( st.countTokens( ) == 2 ) {
								st.nextToken( );// Ignoring the first token.
								res.setContentType( st.nextToken( ) );
							}
							filename = df.getDataPathField();							
						}
					}
				if ( binary ) {
					byte [] data = rs.getBytes(colName);
					if (data == null && filename!=null) {
						byte[] buffer = new byte[1024];
						int bytesRead;
						logger.debug("open file specified in "+filename);
						FileInputStream is= new FileInputStream(new StringBuilder(Mappings.getVSensorConfig(vsName).getStorage().getStorageDirectory()).append("/").append(vsName.substring(0, vsName.indexOf("_"))).append("/").append(rs.getString("DEVICE_ID")).append("/").append(rs.getString(filename)).toString());
						while ((bytesRead=is.read(buffer)) != -1) {
							res.getOutputStream( ).write(buffer, 0, bytesRead);
					    }						
					}
					else {
						res.getOutputStream( ).write( rs.getBytes( colName ) );
					}
				}
				else {
					res.setContentType( "text/xml" );
					res.getWriter( ).write( rs.getString( colName ) );
				}
			}
		} catch (NumberFormatException e1) {
			logger.error("ERROR IN EXECUTING, query: "+query+", colName:"+colName+",primaryKey:"+primaryKey);
			logger.error(e1.getMessage(),e1);
			logger.error("Query is from "+req.getRemoteAddr()+"- "+req.getRemoteHost());
		} catch (SQLException e1) {
			logger.error("ERROR IN EXECUTING, query: "+query+", colName:"+colName+",primaryKey:"+primaryKey);
			logger.error(e1.getMessage(),e1);
			logger.error("Query is from "+req.getRemoteAddr()+"- "+req.getRemoteHost());
		}finally{
			Main.getStorage(vsName).close(conn);
		}
	}

	public void doPost ( HttpServletRequest request , HttpServletResponse response ) throws ServletException , IOException {
		doGet( request , response );
	}

}
