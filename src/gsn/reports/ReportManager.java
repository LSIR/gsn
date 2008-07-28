package gsn.reports;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import gsn.reports.beans.Data;
import gsn.reports.beans.Stream;
import gsn.reports.beans.Report;
import gsn.reports.beans.VirtualSensor;
import org.apache.log4j.Logger;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;

public class ReportManager {	
	
	private static Map<String, String> parameters;
	
	public static transient Logger logger= Logger.getLogger ( ReportManager.class );
	
	public static byte[] generateReport (Collection<Report> reports, String jasperFile, HashMap<String, String> params) {
		byte[] report = null;
		try {
			JasperPrint print = generate (reports, jasperFile, params) ;
			report = JasperExportManager.exportReportToPdf(print);				
		} catch (JRException e) {
			logger.error(e.getMessage(), e);
		}
		return report;
	}
	
	public static void generateReport (Collection<Report> reports, String jasperFile, HashMap<String, String> params, String PDFoutputPath) {
		try {
			JasperPrint print = generate (reports, jasperFile, params) ;
			JasperExportManager.exportReportToPdfFile(print, PDFoutputPath);				
		} catch (JRException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private static JasperPrint generate (Collection<Report> reports, String jasperFile, HashMap<String, String> params) {
		JasperPrint print = null;
		JRBeanCollectionDataSource source = new JRBeanCollectionDataSource (reports) ;	
		HashMap<String, String> finalParameters = defaultParameters();
		finalParameters.putAll(params);	
		try {
			JasperReport report = (JasperReport) JRLoader.loadObjectFromLocation(jasperFile);
			print = JasperFillManager.fillReport(report, parameters, source);
		} catch (JRException e) {
			logger.error(e.getMessage(), e);
		}
		return print;
	}
	
	private static HashMap<String, String> defaultParameters () {
		HashMap<String, String> defaultParameters = new HashMap<String, String> () ;
		defaultParameters.put("SUBREPORT_DIR", "./gsn-reports/compiled/");
		return defaultParameters;
	}

	public static byte[] generateSampleReport () {
		
		// Datas
		long now = System.currentTimeMillis();			
		long twohours = 1000 * 60 * 60 * 2;

		Collection<Data> dat1 = new ArrayList<Data> ();
		dat1.add(new Data(1, (now + twohours),100.0,"label 1"));
		dat1.add(new Data(1, (now + 2 * twohours),90.0,"label 1"));
		dat1.add(new Data(1, (now + 9 * twohours),95.0,"label 1"));
		
		Collection<Data> dat2 = new ArrayList<Data> ();
		dat1.add(new Data(1, (now + twohours),100.0,"label 1"));
		dat1.add(new Data(1, (now + 9 * twohours),95.0,"label 1"));
		
		Date lastUpdate = new Date ();
		
		// Streams
		Collection<Stream> fields_v1 = new ArrayList<Stream> ();
		fields_v1.add(new Stream("Temperature [°C]", lastUpdate.toString(), dat1));
		fields_v1.add(new Stream("Humidity [%]", lastUpdate.toString(), dat2));
		fields_v1.add(new Stream("Solar", lastUpdate.toString(), dat1));
		fields_v1.add(new Stream("Altitude", lastUpdate.toString(), dat1));
		fields_v1.add(new Stream("Length", lastUpdate.toString(), dat1));
		
		Collection<Stream> fields_v2 = new ArrayList<Stream> ();
		fields_v2.add(new Stream("Stream 1", lastUpdate.toString(), dat2));
		fields_v2.add(new Stream("Stream 2", lastUpdate.toString(), dat1));
		
		// Virtual Sensors
		Collection<VirtualSensor> virtualSensors = new ArrayList<VirtualSensor> ();
		virtualSensors.add(new VirtualSensor("Virtual Sensor One","0.0","0.0", fields_v1));
		virtualSensors.add(new VirtualSensor("Virtual Sensor Two","0.0","0.0", fields_v2));
		
		// Report
		Collection<Report> reports = new ArrayList<Report> ();
		reports.add(new Report("montblanc.slf.ch:22001", new Date().toString(), new Date().toString(), new Date().toString(), virtualSensors));

		// Build the source
		
		ReportManager.generateReport(reports, "gsn-reports/compiled/report-default.jasper", new HashMap<String, String>(), "sample-report.pdf");
		byte[] rep = ReportManager.generateReport(reports, "gsn-reports/compiled/report-default.jasper", new HashMap<String, String>());
		return rep;
	}
	
	public static void main (String[] args) {
		generateSampleReport();
		System.out.println("done");
	}
}
