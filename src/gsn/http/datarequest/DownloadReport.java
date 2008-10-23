package gsn.http.datarequest;

import gsn.Mappings;
import gsn.reports.ReportManager;
import gsn.reports.beans.Data;
import gsn.reports.beans.Report;
import gsn.reports.beans.Stream;
import gsn.reports.beans.VirtualSensor;
import gsn.storage.StorageManager;
import java.io.File;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;

public class DownloadReport extends AbstractDataRequest {

	private static final String PARAM_REPORTCLASS 	= "reportclass";

	private static final int[] ALLOWED_REPORT_FIELDS_TYPES = new int[]{ Types.BIGINT, Types.DOUBLE, Types.FLOAT, Types.INTEGER, Types.NUMERIC, Types.REAL, Types.SMALLINT, Types.TINYINT};

	private static transient Logger logger = Logger.getLogger(DownloadReport.class);
	
	private Collection<Report> reports;
	
	private String reportPath;
	
	public DownloadReport(Map<String, String[]> requestParameters) throws DataRequestException {
		super(requestParameters);
	}
	
	@Override
	public void process () throws DataRequestException {
		if (getParameter(requestParameters, PARAM_REPORTCLASS) == null) throw new DataRequestException ("The following >" + PARAM_REPORTCLASS + "< parameter is missing in your query.") ;
		String reportClass = getParameter(requestParameters, PARAM_REPORTCLASS);
		reportPath = "gsn-reports/" + reportClass + ".jasper";
		File f = new File (reportPath) ;
		if (f == null || ! f.exists() || ! f.isFile()) throw new DataRequestException ("The path to compiled jasper file >" + reportPath + "< is not valid.") ;
		//
		reports = new ArrayList<Report> ();
		reports.add(createReport ());
	}
	
	@Override
	public void outputResult (OutputStream os, boolean closeStream) {
		ReportManager.generatePdfReport(reports, reportPath, new HashMap<String, String> (), os);
	}
	
	public byte[] outputResult () {
		return ReportManager.generatePdfReport(reports, reportPath, new HashMap<String, String> ());
	}
	
	private Report createReport () {
		Collection<VirtualSensor> virtualSensors = new ArrayList<VirtualSensor> () ;
		// create all the virtual sensors for the report
		Iterator<Entry<String, FieldsCollection>> iter = getVsnamesAndStreams().entrySet().iterator();
		Entry<String, FieldsCollection> vsNameAndStream;
		VirtualSensor virtualSensor;
		while (iter.hasNext()) {
			vsNameAndStream = iter.next();
			virtualSensor = createVirtualSensor(vsNameAndStream.getKey(), vsNameAndStream.getValue().getFields());
			if (virtualSensor != null) virtualSensors.add(virtualSensor);
		}
		//		
		String aggregationCrierion 	= getAggregationCriterion() 	== null		? "None" 		: getAggregationCriterion().toString();
		String standardCriteria 	= getStandardCriteria() 		== null		? "None" 		: getStandardCriteria().toString();
		String maxNumber			= getLimitCriterion()			== null		? "All"			: getLimitCriterion().getSize().toString();
		
		return new Report (reportPath, (sdf == null ? "UNIX: " + new Date().getTime() : sdf.format(new Date())), aggregationCrierion, standardCriteria,maxNumber, virtualSensors);
	}

	private VirtualSensor createVirtualSensor (String vsname, String[] vsstream) {
		Collection<Stream> streams = null;
		// create all the streams for this Virtual Sensor
		try {
			// Get the last update for this Virtual Sensor (In GSN, all the Virtual Sensor streams are inserted in the same record)
			StringBuilder lastUpdateSb = new StringBuilder().append("select timed from " + vsname + " order by timed limit 1"); //TODO does it work with other DB?
			ResultSet lastUpdateRs = StorageManager.getInstance( ).executeQueryWithResultSet(lastUpdateSb);
			String lastModified;
			if (lastUpdateRs.next()) lastModified = (sdf == null ? "UNIX: " + lastUpdateRs.getLong(1) : sdf.format(new Date(lastUpdateRs.getLong(1))));
			else                     lastModified = "No Data";
			if (lastUpdateRs != null) lastUpdateRs.close();

			// Create the streams
			ResultSet rs = StorageManager.getInstance().executeQueryWithResultSet(getSqlQueries().get(vsname), StorageManager.getInstance().getConnection());
			ResultSetMetaData rsmd = rs.getMetaData();

			Hashtable<String, Stream> dataStreams = new Hashtable<String, Stream> () ;
			FieldsCollection streamNames = getVsnamesAndStreams().get(vsname);
			for (int i = 0 ; i < streamNames.getFields().length ; i++) {
				if (streamNames.getFields()[i].compareToIgnoreCase("timed") != 0 || streamNames.isWantTimed()) {
					dataStreams.put(streamNames.getFields()[i], new Stream (vsstream[i], lastModified, new ArrayList<Data> ()));
				}
			}

			while (rs.next()) {
				Stream astream;
				Integer columnInResultSet;
				for (int i = 0 ; i < vsstream.length ; i++) {
					columnInResultSet = getColumnId (rsmd, vsstream[i]) ;
					if (columnInResultSet != null) {
						if (isAllowedReportType(rsmd.getColumnType(columnInResultSet))) {
							astream = dataStreams.get(vsstream[i]);
							if (astream != null) {
								astream.getDatas().add(new Data ("only",rs.getLong("timed"), rs.getDouble(vsstream[i]), "label"));
							}
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
			logger.debug("The query: ",e);
			return null;
		}
		//
		boolean mappedVirtualSensor = (Mappings.getVSensorConfig(vsname) != null);

		String latitude = "NA";
		if (mappedVirtualSensor && Mappings.getVSensorConfig(vsname).getLatitude() != null) latitude =  Mappings.getVSensorConfig(vsname).getLatitude().toString();
		String longitude = "NA";
		if (mappedVirtualSensor && Mappings.getVSensorConfig(vsname).getLongitude() != null) longitude = Mappings.getVSensorConfig(vsname).getLongitude().toString(); 		
		return new VirtualSensor (
				vsname,
				latitude,
				longitude,
				streams) ;
	}

	private static boolean isAllowedReportType (int type) {
		for (int i = 0 ; i < ALLOWED_REPORT_FIELDS_TYPES.length ; i++) {
			if (type == ALLOWED_REPORT_FIELDS_TYPES[i]) return true;
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
