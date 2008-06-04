package gsn.http;

import gsn.beans.StreamElement;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class MultiDataDownload extends HttpServlet {

	private static transient Logger logger = Logger
	.getLogger(MultiDataDownload.class);

	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		doPost(req, res);
	}

	/**
	 * List of the parameters for the requests: url : /data Example: Getting all
	 * the data in CSV format =>
	 * http://localhost:22001/data?vsName=memoryusage4&fields=heap&display=CSV
	 * another example:
	 * http://localhost:22001/data?vsName=memoryusage4&fields=heap&fields=timed&display=CSV&delimiter=other&otherdelimiter=,
	 * 
	 * param-name: vsName : the name of the virtual sensor we need. param-name:
	 * fields [there can be multiple parameters with this name pointing to
	 * different fields in the stream element]. param-name: commonReq (always
	 * true !) param-name: display , if there is a value it should be CSV.
	 * param-name: delimiter, useful for CSV output (can be
	 * "tab","space","other") param-name: otherdelimiter useful in the case of
	 * having delimiter=other param-name: groupby can point to one of the fields
	 * in the stream element. In case groupby=timed then the parameter
	 * groupbytimed points to the period for which data should be aggregated [in
	 * milliseconds]. param-name: nb give the maximum number of elements to be
	 * outputed (most recent values first). param-name:
	 */
	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws ServletException , IOException {

		SimpleDateFormat sdf = new SimpleDateFormat ("dd/MM/yyyy HH:mm:ss z");
		boolean responseCVS = false;
		boolean wantTimeStamp = false;
		boolean commonReq = true;
		boolean groupByTimed = false;
		PrintWriter respond = res.getWriter();
		int numberSelectedSensor =  Integer.parseInt(HttpRequestUtils.getStringParameter("numberSelectedSensor", null,req));

		String[] vsName = new String[numberSelectedSensor];
		for(int i=0; i < numberSelectedSensor; ++i){
			vsName[i] = HttpRequestUtils.getStringParameter("vsName"+i, null,req);
		}

		responseCVS = true;
		res.setContentType("application/x-download");
		res.setHeader("content-disposition","attachment; filename=data.csv");



		if (req.getParameter("commonReq") != null && req.getParameter("commonReq").equals("false")) {
			commonReq = false;
		}
		String delimiter = ";";
		if (req.getParameter("delimiter") != null && !req.getParameter("delimiter").equals("")) {
			String reqdelimiter = req.getParameter("delimiter");
			if (reqdelimiter.equals("tab")) {
				delimiter = "\t";
			} else if (reqdelimiter.equals("space")){
				delimiter = " ";
			} else if (reqdelimiter.equals("semicolon")){
				delimiter = ";";
			} else if (reqdelimiter.equals("other") && req.getParameter("otherdelimiter") != null && !req.getParameter("otherdelimiter").equals("")) {
				delimiter = req.getParameter("otherdelimiter");
			}
		}
		String generated_request_query = "";
		String expression = "";
		String line="";
		String groupby="";
		String[] fields = req.getParameterValues("fields");
		if (commonReq) {
			if (req.getParameter("fields") != null) {
				for (int i=0; i < fields.length; i++) {
					if (fields[i].equals("timed")) {
						wantTimeStamp = true;
					}
					generated_request_query += ", " + fields[i];
				}    
			}
		} else {
			if (req.getParameter("fields") == null) {
				respond.println("Request ERROR");
				return;
			} else {
				for (int i=0; i < fields.length; i++) {
					if (fields[i].equals("timed")) {
						wantTimeStamp = true;
					}
					generated_request_query += ", " + fields[i];
				}    
			}
			if (req.getParameter("groupby") != null) {
				if (req.getParameter("groupby").equals("timed")) {
					groupByTimed = true;
					int periodmeasure = 1;
					if (req.getParameter("groupbytimed")!=null) {
						periodmeasure = new Integer(req.getParameter("groupbytimed"));
						periodmeasure = java.lang.Math.max(periodmeasure, 1);
					}
					generated_request_query += ", Min(timed), FLOOR(timed/" + periodmeasure + ") period "; 
					groupby = "GROUP BY period";
				} else {
					groupby = "GROUP BY " + req.getParameter("groupby");
				}
			}
		}

		String limit = "";
		if (req.getParameter("nb") != null && req.getParameter("nb") != "") {
			int nb = new Integer(req.getParameter("nb"));
			if (nb > 0) {
				limit = "LIMIT " + nb + "  offset 0";
			}
		}
		String where = "";
		if (req.getParameter("critfield") != null) {
			try {
				String[] critJoin = req.getParameterValues("critJoin");
				String[] neg = req.getParameterValues("neg");
				String[] critfields = req.getParameterValues("critfield");
				String[] critop = req.getParameterValues("critop");
				String[] critval = req.getParameterValues("critval");
				for (int i=0; i < critfields.length ; i++) {
					if (critop[i].equals("LIKE")) {
						if (i > 0) {
							where += " " + critJoin[i-1] + " " + neg[i] + " " + critfields[i] + " LIKE '%"; // +
							// critval[i]
							// +
							// "%'";
						} else {
							where += neg[i] + " " + critfields[i] + " LIKE '%"; // +
							// critval[i]
							// + "%'";
						}
						if (critfields[i].equals("timed")) {
							try {
								Date d = sdf.parse(critval[i]);
								where += d.getTime();
							} catch (Exception e) {
								where += "0";
							}
						} else {
							where += critval[i];
						}
						where += "%'";
					} else {
						if (i > 0) {
							where += " " + critJoin[i-1] + " " + neg[i] + " " + critfields[i] + " " + critop[i] + " "; // critval[i];
						} else {
							where += neg[i] + " " + critfields[i] + " " + critop[i] + " "; // critval[i];
						}
						if (critfields[i].equals("timed")) {
							try {
								Date d = sdf.parse(critval[i]);
								where += d.getTime();
							} catch (Exception e) {
								where += "0";
							}
						} else {
							where += critval[i];
						}
					}
				}
				where = " WHERE " + where;
			} catch (NullPointerException npe) {
				where = " ";
			}
		}

		if (! generated_request_query.equals("")) {
			generated_request_query = generated_request_query.substring(2);


			String finalGeneratedQuest[] = new String[numberSelectedSensor];
			for(int i=0;i<numberSelectedSensor;i++){
				finalGeneratedQuest[i] = "select "+generated_request_query+" from " + vsName[i] + where;
				if (commonReq) {
					finalGeneratedQuest[i] += " order by timed DESC "+limit;
				}
				finalGeneratedQuest[i] += " " + groupby;
				finalGeneratedQuest[i] += ";";
			}
			for (String sql:finalGeneratedQuest)
				System.out.println(">>>"+sql);
			DataEnumerator result[] = new DataEnumerator[numberSelectedSensor];

			for(int i=0;i<numberSelectedSensor;i++){
				line = "";
				try {
					result[i] = StorageManager.getInstance( ).executeQuery( new StringBuilder(finalGeneratedQuest[i]) , false );
				} catch (SQLException e) {
					logger.error("ERROR IN EXECUTING, query: "+finalGeneratedQuest[i]);
					logger.error(e.getMessage(),e);
					logger.error("Query is from "+req.getRemoteAddr()+"- "+req.getRemoteHost());
					return;
				}

				int nbFields = 0;
				boolean firstLine = true;
				respond.println("#"+finalGeneratedQuest[i]);
				while ( result[i].hasMoreElements( ) ) {
					StreamElement se = result[i].nextElement( );
					if (firstLine) {
						nbFields = se.getFieldNames().length;
						if (groupByTimed) {
							nbFields--;
						}
						for (int j=0; j < nbFields; j++){
							// line += delimiter + se.getFieldNames()[i].toString();
							if ((!groupByTimed) || (j != fields.length)) {
								line += delimiter + fields[j];
							} else {
								line += delimiter + "timed";
							}
						}
						if (wantTimeStamp) {
							line += delimiter + "timed";
						}
						firstLine = false;
					}
					//respond.println(line.substring(delimiter.length()));
					line = "";
					for ( int j = 0 ; j < nbFields ; j++ )

						if ( !commonReq && ((j >= fields.length) || (fields[j].contains("timed")))) {
							line += delimiter+sdf.format(se.getData( )[j]);
						} else {
							line += delimiter+se.getData( )[ j ].toString( );
						}
					if (wantTimeStamp) {
						Date d = new Date (se.getTimeStamp());
						line += delimiter + sdf.format(d);
					}
					respond.println(line.substring(delimiter.length()));
				}
				result[i].close();  
			}
		}
	}
}