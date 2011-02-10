package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.VSensorConfig;
import gsn.http.ac.DataSource;
import gsn.http.ac.User;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;

public class XsltServlet extends HttpServlet {
	
	final static String DEFAULT_XSLT_PATH = "xslt";
	
	private static transient Logger logger = Logger.getLogger( XsltServlet.class );

	static final String prefix  = "select * from ";
	static final String postfix = " where PK = ? ";

	public void doGet ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		String vsName = req.getParameter( "vs" );
		String xsltfile = req.getParameter( "xsl" );
        File xsltFile = new File(Main.DEFAULT_WEB_APP_PATH+"/"+DEFAULT_XSLT_PATH+"/"+xsltfile);
        if (!xsltFile.exists()) {
        	logger.debug("could not find xsl file "+xsltFile.getPath());
        }
        else {
        	logger.debug("using xsl file "+xsltFile.getPath());
        }
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
		String colName = req.getParameter( "field" );
		VSensorConfig sensorConfig = Mappings.getVSensorConfig( vsName );
		if ( sensorConfig == null ) {
			res.sendError( WebConstants.ERROR_INVALID_VSNAME , "The specified virtual sensor doesn't exist." );
			return;
		}
		
		logger.debug("transforming vs "+vsName+" field "+colName+" with style "+xsltfile);
		// 			 
        javax.xml.transform.Source xmlSource = null;
        javax.xml.transform.Source xsltSource =
                new javax.xml.transform.stream.StreamSource(xsltFile);
        javax.xml.transform.Result result =
                new javax.xml.transform.stream.StreamResult(res.getOutputStream());
        // create an instance of TransformerFactory
        javax.xml.transform.TransformerFactory transFact =
                javax.xml.transform.TransformerFactory.newInstance(  );
		
        if (colName == null) {
        	xmlSource = new javax.xml.transform.stream.StreamSource(new StringReader(ContainerInfoHandler.buildOutput(vsName, user, false)));
        }
        else {
        	colName = colName.trim( );
        	StringBuilder query;
        	query = new StringBuilder( ).append( prefix ).append( vsName ).append(" where timed = (select max(timed) from " ).append(vsName).append(")");
        	// 	TODO : Check to see if the requested column exists.
        	Connection conn = null;
        	try {
        		conn = Main.getStorage(vsName).getConnection();
        		ResultSet rs = Main.getStorage(vsName).getBinaryFieldByQuery( query , colName , null ,conn);
        		if ( !rs.next() ) {
        			res.sendError( HttpServletResponse.SC_NOT_FOUND , "The requested data is marked as obsolete and is not available." );
        		}else {
        			boolean binary = false;
        			for ( DataField df : sensorConfig.getOutputStructure( ) )
        				if ( df.getName( ).toLowerCase( ).equals( colName.trim( ).toLowerCase( ) ) ) if ( df.getDataTypeID( ) == DataTypes.BINARY ) {
        					StringTokenizer st = new StringTokenizer( df.getType( ) , ":" );
        					binary = true;
        					if ( st.countTokens( ) != 2 ) break;
        					st.nextToken( );// Ignoring the first token.
        					res.setContentType( st.nextToken( ) );
        					// if ( type.equalsIgnoreCase( "svg" ) ) res.setContentType( "" );
        				}
        			if ( binary ) {
        				logger.debug("transforming vs "+vsName+" field "+colName+" with style "+xsltfile);
        				// 			 
        				xmlSource =
        					//new javax.xml.transform.stream.StreamSource(rs.getBytes( colName ).toString());
        					new javax.xml.transform.stream.StreamSource(rs.getBinaryStream(colName));
        			}
        			else {
        				res.sendError( HttpServletResponse.SC_BAD_REQUEST , "The requested datatype is not binary." );
        			}
        		}
        	} catch (NumberFormatException e1) {
        		logger.error("ERROR IN EXECUTING, query: "+query+", colName:"+colName);
        		logger.error(e1.getMessage(),e1);
        		logger.error("Query is from "+req.getRemoteAddr()+"- "+req.getRemoteHost());
        	} catch (SQLException e1) {
        		logger.error("ERROR IN EXECUTING, query: "+query+", colName:"+colName);
        		logger.error(e1.getMessage(),e1);
        		logger.error("Query is from "+req.getRemoteAddr()+"- "+req.getRemoteHost());
        	}finally{
        		Main.getStorage(vsName).close(conn);
        	}
        }

        if (xsltSource != null  && xmlSource!=null) {
        	javax.xml.transform.Transformer trans;
        	try {
        		logger.debug("create transformer.");
        		trans = transFact.newTransformer(xsltSource);
        		logger.debug("transform.");
        		trans.transform(xmlSource, result);
        		logger.debug("transform done.");
        	} catch (TransformerConfigurationException e) {
        		logger.error(e.getMessage());
        		res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR , "Could not transform data." );
        	} catch (TransformerException e) {
        		logger.error(e.getMessage());
        		res.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR , "Could not transform data." );
        	}
        }
	}

	public void doPost ( HttpServletRequest request , HttpServletResponse response ) throws ServletException , IOException {
		doGet( request , response );
	}
}
