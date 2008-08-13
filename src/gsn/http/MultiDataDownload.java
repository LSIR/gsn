package gsn.http;

import gsn.Container;
import gsn.Main;
import gsn.beans.StreamElement;
import gsn.http.datarequest.DataRequest;
import gsn.http.datarequest.AbstractQuery;
import gsn.http.datarequest.DataRequest.FieldsCollection;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class MultiDataDownload extends HttpServlet {

	private static transient Logger logger 			= Logger.getLogger(MultiDataDownload.class);

	private SimpleDateFormat sdf 					= new SimpleDateFormat (Main.getContainerConfig().getTimeFormat());

	private static final String PARAM_OUTPUT_TYPE 	= "outputtype";

	private enum AllowedOutputType {
		csv,
		html
	}

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
		doPost(req, res);
	}

	public void doPost ( HttpServletRequest req , HttpServletResponse res ) throws IOException {
		PrintWriter respond = res.getWriter();
		String outputType = req.getParameter(PARAM_OUTPUT_TYPE);
		AllowedOutputType ot;
		try {
			if (outputType == null) {
				handleError (res, Container.MISSING_VSNAME_ERROR, "The following >" + PARAM_OUTPUT_TYPE + "< parameter is missing in your query >" + req.getQueryString() + "<.");
				return ;
			}
			ot = AllowedOutputType.valueOf(outputType);
		}
		catch (IllegalArgumentException e) {
			handleError(res, Container.MISSING_VSNAME_ERROR, "The >" + outputType + "< output type is not supported.");
			return ;
		}
		try {
			String csvDelimiter	= ";";
			if (ot == AllowedOutputType.csv) {
				res.setContentType("application/x-download");
				res.setHeader("content-disposition","attachment; filename=data.csv");
				//
				if (req.getParameter("delimiter") != null && !req.getParameter("delimiter").equals("")) {
					String reqdelimiter = req.getParameter("delimiter");
					if (reqdelimiter.equals("tab")) {
						csvDelimiter = "\t";
					} else if (reqdelimiter.equals("space")){
						csvDelimiter = " ";
					} else if (reqdelimiter.equals("semicolon")){
						csvDelimiter = ";";
					} else if (reqdelimiter.equals("other") && req.getParameter("otherdelimiter") != null && !req.getParameter("otherdelimiter").equals("")) {
						csvDelimiter = req.getParameter("otherdelimiter");
					}
				}
			}
			else if (ot == AllowedOutputType.html) {
				res.setContentType("text/html");
			}
			//
			DataRequest dr = new DataRequest (req);
			//
			Iterator<Entry<String, AbstractQuery>> iter = dr.getSqlQueries().entrySet().iterator();
			Entry<String, AbstractQuery> nextSqlQuery;
			DataEnumerator de;
			StreamElement se;
			while (iter.hasNext()) {
				nextSqlQuery = iter.next();
				
				de = StorageManager.getInstance().executeQuery(nextSqlQuery.getValue(), false,  StorageManager.getInstance().getConnection());
				//
				if (ot == AllowedOutputType.csv) {
					respond.println("#" + nextSqlQuery.toString());
				}
				else if (ot == AllowedOutputType.html) {
					respond.println("<data>");
				}
				FieldsCollection fc = dr.getVsnamesAndStreams().get(nextSqlQuery.getKey());
				boolean wantTimed = fc != null ? fc.isWantTimed() : false;
				boolean firstLine = true;
				while (de.hasMoreElements()) {
					if (ot == AllowedOutputType.csv) {
						formatCSVElement(respond, de.nextElement(), wantTimed, csvDelimiter, firstLine);
					}
					else if	(ot == AllowedOutputType.html) {
						formatHTMLElement(respond, de.nextElement(), wantTimed, firstLine);
					}
					firstLine = false;
				}
				if (ot == AllowedOutputType.html) respond.println("</data>");	
				de.close();
			}
			respond.flush();
		} catch (ServletException e) {
			handleError(res, Container.UNSUPPORTED_REQUEST_ERROR, e.getMessage());
			return;
		} catch (SQLException e) {
			handleError(res, Container.UNSUPPORTED_REQUEST_ERROR, e.getMessage());
			return;
		}
	}
	
	private static void handleError (HttpServletResponse res, int errorCode, String errorMsg) throws IOException {
		logger.error(errorMsg);
		res.sendError(errorCode, errorMsg);
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
			respond.println("\t<line>");
			for (int i = 0 ; i < se.getData().length ; i++) {
				respond.println("\t\t<field>" + se.getFieldNames()[i].toString()+"</field>");
			}
			if (wantTimed) respond.println("\t\t<field>timed</field>");
			respond.println("\t</line>");
		}
		respond.println("\t<line>");
		for (int i = 0 ; i < se.getData().length ; i++) {
			respond.println("\t\t<field>" + se.getData()[i].toString()+"</field>");
		}
		if (wantTimed) respond.println("\t\t<field>" + sdf.format(new Date(se.getTimeStamp())) + "</field>");
		respond.println("\t</line>");
	}
}