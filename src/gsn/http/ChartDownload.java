package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.reports.ReportManager;
import gsn.reports.beans.Data;
import gsn.reports.beans.Report;
import gsn.reports.beans.Stream;
import gsn.reports.beans.VirtualSensor;
import gsn.storage.StorageManager;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ChartDownload extends HttpServlet {
	
	private static transient Logger logger = Logger.getLogger(ReportDownload.class);

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doPost(req, res);
	}

	/**
	 */
	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		
	}
}
