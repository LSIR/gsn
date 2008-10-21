package gsn.http.datarequest;

import gsn.beans.StreamElement;
import gsn.http.MultiDataDownload;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class DownloadData extends AbstractDataRequest {

	private static transient Logger logger 			= Logger.getLogger(MultiDataDownload.class);

	private static final String PARAM_OUTPUT_TYPE 	= "outputtype";

	public enum AllowedOutputType {
		csv,
		xml
	}

	private AllowedOutputType ot;

	private String csvDelimiter	= ";";

	public DownloadData(Map<String, String[]> requestParameters) throws DataRequestException {
		super(requestParameters);
	}

	@Override
	public void process() throws DataRequestException {
		String outputType = getParameter(requestParameters, PARAM_OUTPUT_TYPE);

		try {
			if (outputType == null) {
				throw new DataRequestException ("The following >" + PARAM_OUTPUT_TYPE + "< parameter is missing in your query.") ;
			}

			ot = AllowedOutputType.valueOf(outputType);

			if (ot == AllowedOutputType.csv) {
				//
				if (getParameter(requestParameters, "delimiter") != null && !getParameter(requestParameters, "delimiter").equals("")) {
					String reqdelimiter = getParameter(requestParameters, "delimiter");
					if (reqdelimiter.equals("tab")) {
						csvDelimiter = "\t";
					} else if (reqdelimiter.equals("space")){
						csvDelimiter = " ";
					} else if (reqdelimiter.equals("semicolon")){
						csvDelimiter = ";";
					} else if (reqdelimiter.equals("other") && getParameter(requestParameters, "otherdelimiter") != null && !getParameter(requestParameters, "otherdelimiter").equals("")) {
						csvDelimiter = getParameter(requestParameters, "otherdelimiter");
					}
				}
			}
		}
		catch (IllegalArgumentException e) {
			throw new DataRequestException ("The >" + outputType + "< output type is not supported.") ;
		}
	}

	public String outputResult () {
		ByteArrayOutputStream baos = new ByteArrayOutputStream () ;
		outputResult(baos);
		return baos.toString();
	}

	@Override
	public void outputResult(OutputStream os) {
		PrintWriter respond = new PrintWriter(os);
		Iterator<Entry<String, AbstractQuery>> iter = getSqlQueries().entrySet().iterator();
		Entry<String, AbstractQuery> nextSqlQuery;
		DataEnumerator de;
		if (ot == AllowedOutputType.xml) {
			respond.println("<result>");
		}
		while (iter.hasNext()) {
			nextSqlQuery = iter.next();
			try {
				de = StorageManager.getInstance().executeQuery(nextSqlQuery.getValue(), false,  StorageManager.getInstance().getConnection());
				logger.debug("Data Enumerator: " + de);
				if (ot == AllowedOutputType.csv) {
					respond.println("#" + nextSqlQuery.getValue().getStandardQuery());
				}
				else if (ot == AllowedOutputType.xml) {
					respond.println("\t<!-- " + nextSqlQuery.getValue().getStandardQuery() + " -->");
					respond.println("\t<data vsname=\"" + nextSqlQuery.getKey() + "\">");
				}
				FieldsCollection fc = getVsnamesAndStreams().get(nextSqlQuery.getKey());
				boolean wantTimed = fc != null ? fc.isWantTimed() : false;
				boolean firstLine = true;
				while (de.hasMoreElements()) {
					if (ot == AllowedOutputType.csv) {
						formatCSVElement(respond, de.nextElement(), wantTimed, csvDelimiter, firstLine);
					}
					else if	(ot == AllowedOutputType.xml) {
						formatHTMLElement(respond, de.nextElement(), wantTimed, firstLine);
					}
					firstLine = false;
				}
				if (ot == AllowedOutputType.xml) respond.println("\t</data>");	
			} catch (SQLException e) {
				logger.debug(e.getMessage());
			}
		}
		if (ot == AllowedOutputType.xml) {
			respond.println("</result>");
		}
		respond.flush();
	}


	private void formatCSVElement (PrintWriter respond, StreamElement se, boolean wantTimed, String cvsDelimiter, boolean firstLine) {
		if (firstLine) {
			respond.print("#");
			for (int i = 0 ; i < se.getData().length ; i++) {
				respond.print(se.getFieldNames()[i].toString());
				if (i != se.getData().length - 1) respond.print(cvsDelimiter);
			}
			if (wantTimed) respond.print(cvsDelimiter + "timed");
			respond.println();
		}
		for (int i = 0 ; i < se.getData().length ; i++) {
			respond.print(se.getData()[i].toString());
			if (i != se.getData().length - 1) respond.print(cvsDelimiter); 
		}
		if (wantTimed) respond.print(sdf.format(new Date(se.getTimeStamp())));
		respond.println();
	}

	private void formatHTMLElement (PrintWriter respond, StreamElement se, boolean wantTimed, boolean firstLine) {
		if (firstLine) {
			respond.println("\t\t<line>");
			for (int i = 0 ; i < se.getData().length ; i++) {
				respond.println("\t\t\t<field>" + se.getFieldNames()[i].toString()+"</field>");
			}
			if (wantTimed) respond.println("\t\t\t<field>timed</field>");
			respond.println("\t\t</line>");
		}
		respond.println("\t\t<line>");
		for (int i = 0 ; i < se.getData().length ; i++) {
			respond.println("\t\t\t<field>" + se.getData()[i].toString()+"</field>");
		}
		if (wantTimed) respond.println("\t\t<field>" + sdf.format(new Date(se.getTimeStamp())) + "</field>");
		respond.println("\t\t</line>");
	}

	public AllowedOutputType getOt() {
		return ot;
	}
}
