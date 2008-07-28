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
import java.sql.Types;

public class ReportDownload extends HttpServlet {

	private static transient Logger logger = Logger.getLogger(ReportDownload.class);

	private static SimpleDateFormat sdf = new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());

	private long startTime;
	private long endTime;
	private String reportClass;
	private HashMap<String, String[]> virtualSensorsNamesAndStreams;

	private static final String PARAM_VSNAME = "vsname";
	private static final String PARAM_STARTTIME = "starttime";
	private static final String PARAM_ENDTIME = "endtime";
	private static final String PARAM_REPORTCLASS = "reportclass";

	private static final int[] ALLOWED_REPORT_TYPES = new int[]{ Types.BIGINT, Types.DOUBLE, Types.FLOAT, Types.INTEGER, Types.NUMERIC, Types.REAL, Types.SMALLINT, Types.TINYINT};

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		doPost(req, res);
	}

	/**
	 * TODO Define the request structure
	 * 
	 * eg. /report?vsname=tramm_meadows_vs:toppvwc_1:toppvwc_3:toppvwc_6&vsname=ss_mem_vs:heap_memory_usage:non_heap_memory_usage:pending_finalization_count&starttime=1105436165944&endtime=1216908392125&reportclass=report-default
	 */
	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {
		//
		parseParameters (req);
		//
		Collection<Report> reports = new ArrayList<Report> ();
		reports.add(createReport ());
		//
		byte[] pdf = ReportManager.generateReport(reports, "gsn-reports/compiled/" + reportClass + ".jasper", new HashMap<String, String>());
		res.setContentType("application/pdf");
		res.setHeader("content-disposition","attachment; filename=sample-report.pdf");
		res.setContentLength(pdf.length);
		res.getOutputStream().write(pdf);
		res.getOutputStream().flush();
	}

	private Report createReport () {
		Collection<VirtualSensor> virtualSensors = new ArrayList<VirtualSensor> () ;
		// create all the virtual sensors for the report
		Iterator<Entry<String, String[]>> iter = virtualSensorsNamesAndStreams.entrySet().iterator();
		Entry<String, String[]> vsNameAndStream;
		VirtualSensor virtualSensor;
		while (iter.hasNext()) {
			vsNameAndStream = iter.next();
			virtualSensor = createVirtualSensor(vsNameAndStream.getKey(), vsNameAndStream.getValue());
			if (virtualSensor != null) virtualSensors.add(virtualSensor);		
		}
		//
		logger.debug("New Report Created");
		return new Report ("", sdf.format(new Date()), sdf.format(new Date(startTime)), sdf.format(new Date(endTime)), virtualSensors) ;
	}

	private VirtualSensor createVirtualSensor (String vsname, String[] vsstream) {
		Collection<Stream> streams = null;
		// create all the streams for this Virtual Sensor
		StringBuilder sqlRequest = null;
		try {
			// Get the last update for this Virtual Sensor (In GSN, all the Virtual Sensor streams are inserted in the same record)
			StringBuilder lastUpdateSb = new StringBuilder().append("select timed from " + vsname + " order by timed limit 1"); //TODO does it work with other DB?
			ResultSet lastUpdateRs = StorageManager.getInstance( ).executeQueryWithResultSet(lastUpdateSb);
			String lastModified;
			if (lastUpdateRs.next()) lastModified = sdf.format(new Date(lastUpdateRs.getLong(1)));
			else                     lastModified = "No Data";
			if (lastUpdateRs != null) lastUpdateRs.close();
			
			// Create the streams
			sqlRequest = generateVirtualSensorRequest (vsname, vsstream) ;
			ResultSet rs = StorageManager.getInstance( ).executeQueryWithResultSet(sqlRequest);
			ResultSetMetaData rsmd = rs.getMetaData();

			Hashtable<String, Stream> dataStreams = new Hashtable<String, Stream> () ;
			String[] streamNames = virtualSensorsNamesAndStreams.get(vsname);
			for (int i = 0 ; i < streamNames.length ; i++) {
				dataStreams.put(streamNames[i], new Stream (vsstream[i], lastModified, new ArrayList<Data> ()));
			}

			while (rs.next()) {
				Stream astream;
				Integer columnInResultSet;
				for (int i = 0 ; i < vsstream.length ; i++) {
					columnInResultSet = getColumnId (rsmd, vsstream[i]) ;
					if (columnInResultSet != null) {
						if (isAllowedReportType(rsmd.getColumnType(columnInResultSet))) {
							astream = dataStreams.get(vsstream[i]);
							if (astream == null) {
								logger.error("Got a non requested field from the DATABASE, check SQL QUERY");
							}
							astream.getDatas().add(new Data ("only",rs.getLong("timed"), rs.getDouble(vsstream[i]), "label"));
						}
						else logger.debug("Column type >" + rsmd.getColumnType(columnInResultSet) + "< is not allowed for report.");
					}
					else logger.debug("Column >" + vsstream[i] + "< not found in the ResultSetMetaData");
				}
			}
			if (rs != null) rs.close(); 
			streams = dataStreams.values();
		}
		catch (SQLException e) {
			logger.error("Error while executing the SQL request. Check your query.");
			return null;
		}
		//
		logger.debug("New Virtual Sensor Report created");
		String latitude = "NA";
		if (Mappings.getVSensorConfig(vsname).getLatitude() != null) latitude =  Mappings.getVSensorConfig(vsname).getLatitude().toString();
		String longitude = "NA";
		if (Mappings.getVSensorConfig(vsname).getLongitude() != null) longitude = Mappings.getVSensorConfig(vsname).getLongitude().toString(); 		
		return new VirtualSensor (
				vsname,
				latitude,
				longitude,
				streams) ;
	}

	private boolean parseParameters (HttpServletRequest req) throws ServletException {
		if (
				req.getParameter(PARAM_VSNAME) == null ||
				req.getParameter(PARAM_STARTTIME) == null ||
				req.getParameter(PARAM_ENDTIME) == null ||
				req.getParameter(PARAM_REPORTCLASS) == null
		) throw new ServletException ("Wrong request. Check your query >" + req.getQueryString() + "<.") ;
		//
		startTime = Long.parseLong(req.getParameterValues(PARAM_STARTTIME)[0]);
		//
		endTime = Long.parseLong(req.getParameterValues(PARAM_ENDTIME)[0]);
		//
		reportClass = req.getParameterValues(PARAM_REPORTCLASS)[0];
		//
		virtualSensorsNamesAndStreams = new HashMap<String, String[]> () ;
		String[] vsnames = req.getParameterValues(PARAM_VSNAME);
		String name = null;
		String[] streams = null;
		int firstColumnIndex;
		for (int i = 0 ; i < vsnames.length ; i++) {
			firstColumnIndex = vsnames[i].indexOf(':');
			if (firstColumnIndex == -1) {
				name = vsnames[i];
				streams = new String[0];
			}
			else {
				name = vsnames[i].substring(0, firstColumnIndex);
				streams = vsnames[i].substring(firstColumnIndex + 1).split(":");

			}
			virtualSensorsNamesAndStreams.put(name, streams);
		}
		return true;
	}

	private StringBuilder generateVirtualSensorRequest (String vsname, String[] streamnames) {
		StringBuilder sb = new StringBuilder () ;
		sb.append("select ");
		for (int i = 0 ; i < streamnames.length ; i++) {
			sb.append(streamnames[i]);
			if (i < streamnames.length - 1) sb.append(",");                          
		}
		if (! sb.toString().toUpperCase().contains("TIMED")) sb.append(",timed");
		sb.append(" ");
		sb.append("from ");
		sb.append(vsname + " ");
		sb.append("where timed < ");
		sb.append(endTime + " ");
		sb.append("and timed >= ");
		sb.append(startTime + " ");
		sb.append("order by timed; ");
		logger.debug("Generated request: >" + sb.toString() + "<");
		return sb;
	}

	private static boolean isAllowedReportType (int type) {
		for (int i = 0 ; i < ALLOWED_REPORT_TYPES.length ; i++) {
			if (type == ALLOWED_REPORT_TYPES[i]) return true;
		}
		return false;
	}

	private static Integer getColumnId (ResultSetMetaData rsmd, String columnname) {
		try {
			for (int i = 1 ; i <= rsmd.getColumnCount() ; i++) {
				if (rsmd.getColumnName(i).compareToIgnoreCase(columnname) == 0) return i;
			}
		}
		catch (SQLException e) {
			logger.error(e.getMessage(), e);
		} 
		return null;
	}
}
