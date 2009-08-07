package gsn.http;

import gsn.http.datarequest.DataRequestException;
import gsn.http.datarequest.DownloadReport;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * 
 */
public class MultiDataDownload extends HttpServlet {

	private static final long serialVersionUID = 4249739276150343437L;

	private static SimpleDateFormat sdfWeb = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss") ; // 29/10/2008 22:25:07

	private static transient Logger logger = Logger.getLogger(MultiDataDownload.class);

	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws IOException {
		doPost(req, res);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws IOException {
		try {
			logger.debug("Query string: " + req.getQueryString());

			String downloadFormat = req.getParameter("download_format");
			Map<String, String[]> parameterMap = parseParameters(req);
			if (downloadFormat.compareTo("csv") == 0) {
				gsn.http.datarequest.DownloadData dd = new gsn.http.datarequest.DownloadData(parameterMap);
				dd.process();
				res.setContentType("application/x-download");
				res.setHeader("content-disposition","attachment; filename=data.csv");
				dd.outputResult(res.getOutputStream());
				//res.getOutputStream().flush();
			}
			else if (downloadFormat.compareTo("xml") == 0) {
				gsn.http.datarequest.DownloadData dd = new gsn.http.datarequest.DownloadData(parameterMap);
				dd.process();
				res.setContentType("text/xml");
				res.setHeader("content-disposition","attachment; filename=data.xml");
				dd.outputResult(res.getOutputStream());
				//res.getOutputStream().flush();
			}
			else if (downloadFormat.compareTo("pdf") == 0) {
				DownloadReport rpd = new DownloadReport (parameterMap) ;
				rpd.process();
				res.setContentType("application/pdf");
				res.setHeader("content-disposition","attachment; filename=data.pdf");
				rpd.outputResult(res.getOutputStream());
				res.getOutputStream().flush();
			}
			else {
				throw new DataRequestException("Unknown download_format >" + downloadFormat + "<");
			}
		} catch (DataRequestException e) {
			logger.error(e.getMessage());
			res.sendError(WebConstants.ERROR_INVALID_VSNAME, e.getMessage());
			return;
		}
	}
	
	private Map<String, String[]> parseParameters (HttpServletRequest req) {
		
		Map<String, String[]> parameterMap = new Hashtable<String, String[]>();

		Hashtable<String, ArrayList<String>> vssfm = buildVirtualSensorsFieldsMapping(req.getParameterMap());

		// VS
		Iterator<Entry <String, ArrayList<String>>> vsAndFields = vssfm.entrySet().iterator();
		Iterator<String> fieldsIterator;
		Entry<String, ArrayList<String>> vsAndFieldsEntry;
		StringBuilder vsname ;
		ArrayList<String> vsnames = new ArrayList<String> () ;
		while (vsAndFields.hasNext()) {
			vsAndFieldsEntry = vsAndFields.next();
			vsname = new StringBuilder();
			vsname.append(vsAndFieldsEntry.getKey());
			fieldsIterator = vsAndFieldsEntry.getValue().iterator();
			while (fieldsIterator.hasNext()) {
				vsname.append(":");
				vsname.append(fieldsIterator.next());
			}
			vsnames.add(vsname.toString());
		}
		parameterMap.put("vsname", vsnames.toArray(new String[] {}));

		
		// TIME FORMAT
		String req_time_format = req.getParameter("time_format");
		if (req_time_format != null) {
			parameterMap.put("timeformat", new String[] {req_time_format});
		}
		
		// Download format
		String req_download_format = req.getParameter("download_format");
		if (req_download_format != null) {
			parameterMap.put("outputtype", new String[] { req_download_format });
		}
		else {
			parameterMap.put("outputtype", new String[] { "csv" });
		}
		
		// CRITFIELDS
		// TIME LIMITS
		ArrayList<String> critFields = new ArrayList<String> () ;
		String req_from = req.getParameter("from");
		String req_to = req.getParameter("to");
		Date timeLimit;
		try {
			if (req_from != null) {
				timeLimit = sdfWeb.parse(req_from);
				vsAndFields = vssfm.entrySet().iterator();
				while (vsAndFields.hasNext()) {
					vsAndFieldsEntry = vsAndFields.next();
					critFields.add("and::" + vsAndFieldsEntry.getKey() + ":timed:ge:" + timeLimit.getTime());
				}
			}
		} catch (ParseException e1) {
			logger.debug(e1.getMessage());
		}
		try {
			if (req_to != null) {
				timeLimit = sdfWeb.parse(req_to);
				vsAndFields = vssfm.entrySet().iterator();
				while (vsAndFields.hasNext()) {
					vsAndFieldsEntry = vsAndFields.next();
					critFields.add("and::" + vsAndFieldsEntry.getKey() + ":timed:leq:" + timeLimit.getTime());
				}
			}
		} catch (ParseException e1) {
			logger.debug(e1.getMessage());
		}

		// CONDITIONS
		Hashtable<String, String> cVss = buildWebParameterMapping("c_vs[", req.getParameterMap());
		Hashtable<String, String> cJoins = buildWebParameterMapping("c_join[", req.getParameterMap());
		Hashtable<String, String> cFields = buildWebParameterMapping("c_field[", req.getParameterMap());
		Hashtable<String, String> cMins = buildWebParameterMapping("c_min[", req.getParameterMap());
		Hashtable<String, String> cMaxs = buildWebParameterMapping("c_max[", req.getParameterMap());
		Iterator<Entry <String, String>> iter = cJoins.entrySet().iterator();
		Entry<String, String> entry;
		while (iter.hasNext()) {
			entry = iter.next();
			String cField = cFields.get(entry.getKey());
			String cVs = cVss.get(entry.getKey());
			String cJoin = cJoins.get(entry.getKey());
			String cMin = cMins.get(entry.getKey());
			String cMax = cMaxs.get(entry.getKey());

			// VS and Fields
			Hashtable<String, ArrayList<String>> vsAndFieldSelected = new Hashtable<String, ArrayList<String>> ();
			if (cVs.compareToIgnoreCase("All") == 0) {
				if (cField.compareToIgnoreCase("All") == 0) {
					vsAndFieldSelected = vssfm;
				}
				else {
					vsAndFieldSelected = (Hashtable<String, ArrayList<String>>) vssfm.clone();
					ArrayList<String> toRetain = new ArrayList<String> () ;
					toRetain.add(cField);
					vsAndFieldSelected.values().retainAll(toRetain);
				}
			}
			else {
				if (cField.compareToIgnoreCase("All") == 0) {
					vsAndFieldSelected.put(cVs, vssfm.get(cVs));
				}
				else {
					ArrayList<String> tmp = new ArrayList<String> () ;
					tmp.add(cField);
					vsAndFieldSelected.put(cVs, tmp);
				}
			}

			Iterator<Entry <String,ArrayList<String>>> vsAndFieldsIterator = vsAndFieldSelected.entrySet().iterator();
			Iterator<String> fieldIterator;
			Entry<String, ArrayList<String>> entry2 ;
			String fieldName;
			String vsName;
			while (vsAndFieldsIterator.hasNext()) {
				entry2 = vsAndFieldsIterator.next();
				fieldIterator = entry2.getValue().iterator();
				while (fieldIterator.hasNext()) {
					vsName = entry2.getKey();
					fieldName = fieldIterator.next();
					// Mins
					if (cMin.compareToIgnoreCase("-inf") != 0) {
						//criteria.add(cjoins[i] + "::" + vsnames[j] + ":" + cfields[i] + ":ge:" + cmins[i]);
						critFields.add(cJoin + "::" + vsName + ":" + fieldName + ":ge:" + cMin);
					}
					// Maxs
					if (cMax.compareToIgnoreCase("+inf") != 0) {
						critFields.add(cJoin + "::" + vsName + ":" + fieldName + ":leq:" + cMax);
					}		
				}
			}
		}
		//
		parameterMap.put("critfield", critFields.toArray(new String[] {}));


		// NB
		String req_nb = req.getParameter("nb");
		if (req_nb != null) {
			if (req_nb.compareToIgnoreCase("SPECIFIED") == 0) {
				String req_nb_value = req.getParameter("nb_value");
				try {
					Integer checked_nb = Integer.parseInt(req_nb_value);
					parameterMap.put("nb", new String[] { "0:" + checked_nb });
				}
				catch (NumberFormatException e1) {
					logger.debug("The specified nb of data >" + req_nb_value + "< is not a number.");
				}
			}
		}

		// AGGREGATION
		String req_agg_function = req.getParameter("agg_function");
		if (req_agg_function != null && req_agg_function.compareToIgnoreCase("-1") != 0) {
			String req_agg_period = req.getParameter("agg_period");
			String req_agg_unit = req.getParameter("agg_unit");
			try {
				long timerange = Long.parseLong(req_agg_unit) * Long.parseLong(req_agg_period);
				parameterMap.put("groupby", new String[] { timerange + ":" + req_agg_function });
			}
			catch (NumberFormatException e2) {
				logger.debug(e2);
			}
		}
		
		// REPORT TEMPLATE
		String req_reportclass = req.getParameter("reportclass");
		if (req_reportclass != null) {
			parameterMap.put("reportclass", new String[] { req_reportclass });
		}
		
		return parameterMap;
	}


	private Hashtable<String, ArrayList<String>> buildVirtualSensorsFieldsMapping (Map<String, String[]> pm) {

		Hashtable<String, ArrayList<String>> vssfm = new Hashtable<String, ArrayList<String>> () ;
		//
		Hashtable<String, ArrayList<String>> allVsAndFieldsMapping = buildAllVirtualSensorsAndFieldsMapping () ;
		//
		Hashtable<String, String> vsnames = buildWebParameterMapping("vs[", pm); 	// key = [x], value = vsname
		Hashtable<String, String> fields = buildWebParameterMapping("field[", pm); 	// key = [x], value = fieldname
		//
		Iterator<Entry <String, String>> iter2 = vsnames.entrySet().iterator();
		String vsname;
		String field;
		Set<Entry <String, ArrayList<String>>> entries ;
		Entry<String, String> en2;
		ArrayList<String> availableFields;
		while (iter2.hasNext()) {
			en2 = iter2.next();
			vsname = (String) en2.getValue();
			field = fields.get(en2.getKey());
			if (vsname.compareToIgnoreCase("All") == 0) {
				entries = allVsAndFieldsMapping.entrySet();
				Iterator<Entry<String, ArrayList<String>>> inneriter = entries.iterator();
				Entry<String, ArrayList<String>> innerentry;
				if (field.compareToIgnoreCase("All") == 0) {
					while (inneriter.hasNext()) {
						innerentry = inneriter.next();
						updateMapping(vssfm, (String)innerentry.getKey(), (ArrayList<String>)innerentry.getValue());	
					}
				}
				else {
					while (inneriter.hasNext()) {
						innerentry = inneriter.next();
						availableFields = allVsAndFieldsMapping.get((String)innerentry.getKey());
						if (availableFields != null && availableFields.contains(field)) {
							updateMapping(vssfm, (String)innerentry.getKey(), field);	
						}
					}
				}
			}
			else {
				if (field.compareToIgnoreCase("All") == 0) {
					updateMapping(vssfm, vsname, allVsAndFieldsMapping.get(vsname));	
				}
				else {
					updateMapping(vssfm, vsname, field);	
				}
			}
		}
		return vssfm;
	}

	private Hashtable<String, ArrayList<String>> buildAllVirtualSensorsAndFieldsMapping () {
		//
		Hashtable<String, ArrayList<String>> allVsAndFieldsMapping = new Hashtable<String, ArrayList<String>>();
//		Iterator<VSFile> iter = Mappings.getAllVSensorConfigs();
//		VSFile vsc ;
		ArrayList<String> allFields;
//		while (iter.hasNext()) {
//			vsc = (VSFile) iter.next();
//			allFields = new ArrayList<String> () ;
//			DataField[] dfs = vsc.getProcessingClassConfig().getOutputFormat();
//			for (int i = 0 ; i < dfs.length ; i++) {
//				allFields.add(dfs[i].getName());
//			}	
//			allVsAndFieldsMapping.put(vsc.getName(), allFields);
//		}
		return allVsAndFieldsMapping;
	}

	private void updateMapping (Hashtable<String, ArrayList<String>> vssfm, String vsname, String field) {
		ArrayList<String> tmp = new ArrayList<String> () ;
		tmp.add(field);
		updateMapping(vssfm, vsname, tmp);
	}

	private void updateMapping (Hashtable<String, ArrayList<String>> vssfm, String vsname, ArrayList<String> fields) {
		if ( ! vssfm.containsKey(vsname)) {
			vssfm.put(vsname, new ArrayList<String>());
		}
		ArrayList<String> vsnameFields = vssfm.get(vsname);
		Iterator<String> iter = fields.iterator();		
		String fieldName ;
		while (iter.hasNext()) {
			fieldName = (String) iter.next();
			if (! vsnameFields.contains(fieldName)) {
				vsnameFields.add(fieldName);
			}
		}
		vssfm.put(vsname, vsnameFields);
	}

	private Hashtable<String, String> buildWebParameterMapping(String prefix, Map<String, String[]> pm) {
		Hashtable<String, String> mapping = new Hashtable<String, String> () ;
		Set<Entry <String,String[]>> sp = pm.entrySet();
		Iterator<Entry <String,String[]>> iter = sp.iterator();
		Entry<String, String[]> en;
		String key;
		while (iter.hasNext()) {
			en = iter.next();
			key = (String) en.getKey() ;
			if (key.length() > prefix.length() && key.substring(0,prefix.length()).compareToIgnoreCase(prefix) == 0) {	// look for "vs["
				String[] vals = (String[]) en.getValue();
				mapping.put(key.substring(prefix.length() - 1), vals[0]);
			}
		}
		return mapping;
	}

	private void plotMapping (Hashtable<String, ArrayList<String>> vssfm) {
		Iterator<Entry <String, ArrayList<String>>> myiter = vssfm.entrySet().iterator();
		Entry<String, ArrayList<String>> myentry;
		while (myiter.hasNext()) {
			myentry = myiter.next();
			System.out.println("VSNAME: " + myentry.getKey());
			Iterator<String> inneriter = myentry.getValue().iterator();
			while (inneriter.hasNext()) {
				System.out.println("FIELD: " + inneriter.next());
			}
		}
	}

}