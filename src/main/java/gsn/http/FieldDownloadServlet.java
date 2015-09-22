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
* File: src/gsn/http/FieldDownloadServlet.java
*
* @author gsn_devs
* @author Sofiane Sarni
* @author Mehdi Riahi
* @author Ali Salehi
* @author Timotee Maret
*
*/

package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataField;
import gsn.beans.DataTypes;
import gsn.beans.VSensorConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import gsn.http.ac.DataSource;
import gsn.http.ac.User;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

//path="/WEB-INF/file_not_found.jpg"

public class FieldDownloadServlet extends HttpServlet {

	private static transient Logger                                      logger                             = LoggerFactory.getLogger( FieldDownloadServlet.class );

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
		// TODO : Check to see if the requested column exists.
		StringBuilder query = new StringBuilder( ).append( prefix ).append( vsName ).append( postfix );
		Connection conn = null;
		try {
			conn = Main.getStorage(vsName).getConnection();
			ResultSet rs = Main.getStorage(vsName).getBinaryFieldByQuery( query , colName , Long.parseLong( primaryKey ) ,conn);
			if ( !rs.next() ) {
				res.sendError( res.SC_NOT_FOUND , "The requested data is marked as obsolete and is not available." );
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
				if ( binary )
					res.getOutputStream( ).write( rs.getBytes( colName ) );
				else {
					res.setContentType( "text/xml" );
					res.getWriter( ).write( rs.getString( colName ) );
				}
			}
		} catch (NumberFormatException e1) {
			logger.error("ERROR IN EXECUTING, query: "+query+", colName:"+colName+",primaryKey:"+primaryKey+" from "+req.getRemoteAddr()+"- "+req.getRemoteHost()+": "+e1.getMessage());
		} catch (SQLException e1) {
			logger.error("ERROR IN EXECUTING, query: "+query+", colName:"+colName+",primaryKey:"+primaryKey+" from "+req.getRemoteAddr()+"- "+req.getRemoteHost()+": "+e1.getMessage());
		}finally{
			Main.getStorage(vsName).close(conn);
		}
	}

	public void doPost ( HttpServletRequest request , HttpServletResponse response ) throws ServletException , IOException {
		doGet( request , response );
	}

}
