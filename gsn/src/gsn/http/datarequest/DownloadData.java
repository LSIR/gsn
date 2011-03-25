package gsn.http.datarequest;

import gsn.Main;
import gsn.beans.StreamElement;
import gsn.http.MultiDataDownload;
import gsn.storage.DataEnumerator;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public class DownloadData extends AbstractDataRequest {

    private static transient Logger logger = Logger.getLogger(MultiDataDownload.class);

    private static final String PARAM_OUTPUT_TYPE = "outputtype";

    public enum AllowedOutputType {
        csv,
        xml
    }

    private AllowedOutputType ot;

    private String csvDelimiter = ",";
    
    private String timedfield;

    public DownloadData(Map<String, String[]> requestParameters) throws DataRequestException {
        super(requestParameters);
        timedfield = QueriesBuilder.getParameter(requestParameters, QueriesBuilder.PARAM_TIME_LINE);
    }

    @Override
    public void process() throws DataRequestException {
        String outputType = QueriesBuilder.getParameter(requestParameters, PARAM_OUTPUT_TYPE);

        try {
            if (outputType == null) {
                throw new DataRequestException("The following >" + PARAM_OUTPUT_TYPE + "< parameter is missing in your query.");
            }

            ot = AllowedOutputType.valueOf(outputType);

            if (ot == AllowedOutputType.csv) {
                //
                if (QueriesBuilder.getParameter(requestParameters, "delimiter") != null && !QueriesBuilder.getParameter(requestParameters, "delimiter").equals("")) {
                    String reqdelimiter = QueriesBuilder.getParameter(requestParameters, "delimiter");
                    if (reqdelimiter.equals("tab")) {
                        csvDelimiter = "\t";
                    } else if (reqdelimiter.equals("space")) {
                        csvDelimiter = " ";
                    } else if (reqdelimiter.equals("semicolon")) {
                        csvDelimiter = ";";
                    } else if (reqdelimiter.equals("other") && QueriesBuilder.getParameter(requestParameters, "otherdelimiter") != null && !QueriesBuilder.getParameter(requestParameters, "otherdelimiter").equals("")) {
                        csvDelimiter = QueriesBuilder.getParameter(requestParameters, "otherdelimiter");
                    }
                }
            }
        }
        catch (IllegalArgumentException e) {
            throw new DataRequestException("The >" + outputType + "< output type is not supported.");
        }
    }

//    public String outputResult() {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        outputResult(baos);
//        return baos.toString();
//    }

    @Override
    public void outputResult(OutputStream os) {
        PrintWriter respond = new PrintWriter(os);
        Iterator<Entry<String, AbstractQuery>> iter = qbuilder.getSqlQueries().entrySet().iterator();
        Entry<String, AbstractQuery> nextSqlQuery;
        DataEnumerator de = null;
        try {
            if (ot == AllowedOutputType.xml) {
                respond.println("<result>");
            }
            while (iter.hasNext()) {
                nextSqlQuery = iter.next();
                Connection connection = null;

                connection = Main.getStorage(nextSqlQuery.getKey()).getConnection();
                de = Main.getStorage(nextSqlQuery.getKey()).streamedExecuteQuery(nextSqlQuery.getValue(), true, timedfield, connection);
                
                logger.debug("Data Enumerator: " + de);
                int pkIndex = -1;
                int i=0;
                for (String field: nextSqlQuery.getValue().getFields()) {
                	if (field.equalsIgnoreCase("pk")) {
                		pkIndex=i;
                		break;
                	}
                	i++;
                }
                if (ot == AllowedOutputType.csv) {
                    respond.println("##vsname:" + nextSqlQuery.getKey());
                    respond.println("##query:" + nextSqlQuery.getValue().getStandardQuery() + (nextSqlQuery.getValue().getLimitCriterion() == null ? "" : "(" + nextSqlQuery.getValue().getLimitCriterion() + ")"));
                } else if (ot == AllowedOutputType.xml) {
                    respond.println("\t<!-- " + nextSqlQuery.getValue().getStandardQuery() + " -->");
                    respond.println("\t<data vsname=\"" + nextSqlQuery.getKey() + "\">");
                }
                boolean wantTimed = true;
                boolean firstLine = true;
                while (de.hasMoreElements()) {
                    if (ot == AllowedOutputType.csv) {
                        formatCSVElement(respond, de.nextElement(), wantTimed, pkIndex, csvDelimiter, firstLine);
                    } else if (ot == AllowedOutputType.xml) {
                        formatXMLElement(respond, de.nextElement(), wantTimed, pkIndex, firstLine);
                    }
                    firstLine = false;
                }
                if (ot == AllowedOutputType.xml)
                    respond.println("\t</data>");
            }
            if (ot == AllowedOutputType.xml) {
                respond.println("</result>");
            }
        } catch (SQLException e) {
            logger.debug(e.getMessage());
        } finally {
            respond.flush();
            if (de != null)
                de.close();
        }
    }


    private void formatCSVElement(PrintWriter respond, StreamElement se, boolean wantTimed, int pkIndex,  String cvsDelimiter, boolean firstLine) {
    	boolean firstel;
        if (firstLine) {
            respond.print("#");
            firstel = true;
            int i = 0;
            for (; i < se.getData().length; i++) {
            	if (i == pkIndex) {
            		if (!firstel)
                		respond.print(cvsDelimiter);
            		respond.print("pk");
            		firstel = false;
            	}
            	if (!firstel)
            		respond.print(cvsDelimiter);            	
           	    respond.print(se.getFieldNames()[i]);
           	    firstel = false;
            }
            if (i == pkIndex) {
        		if (!firstel)
            		respond.print(cvsDelimiter);
        		respond.print("pk");
        		firstel = false;
        	}
            if (wantTimed) {
            	if (!firstel)
            		respond.print(cvsDelimiter);       
            	respond.print(timedfield);
            }
            respond.println();
        }
        firstel = true;
        int i = 0;
        for (;i < se.getData().length; i++) {
        	if (i == pkIndex) {
        		if (!firstel)
            		respond.print(cvsDelimiter);
        		respond.print(se.getInternalPrimayKey());
        		firstel = false;
        	}
        	if (!firstel)
        		respond.print(cvsDelimiter);            	
       	    respond.print(se.getData()[i]);
       	    firstel = false;
        }
        if (i == pkIndex) {
    		if (!firstel)
        		respond.print(cvsDelimiter);
    		respond.print(se.getInternalPrimayKey());
    		firstel = false;
    	}
        if (wantTimed) {
        	if (!firstel)
        		respond.print(cvsDelimiter);       
        	respond.print(qbuilder.getSdf() == null ? timestampInUTC(se.getTimeStamp()) : qbuilder.getSdf().format(new Date(se.getTimeStamp())));
        }
        respond.println();
    }

    private void formatXMLElement(PrintWriter respond, StreamElement se, boolean wantTimed, int pkIndex, boolean firstLine) {
        if (firstLine) {
            respond.println("\t\t<header>");
            for (int i = 0; i < se.getData().length; i++) {
            	if (i == pkIndex)
            		respond.println("\t\t\t<field>pk</field>");
                respond.println("\t\t\t<field>" + se.getFieldNames()[i] + "</field>");
            }
            if (wantTimed)
                respond.println("\t\t\t<field>"+timedfield+"</field>");
            respond.println("\t\t</header>");
        }
        respond.println("\t\t<tuple>");
        for (int i = 0; i < se.getData().length; i++) {
        	if (i == pkIndex)
        		respond.println("\t\t\t<field>"+se.getInternalPrimayKey()+"</field>");
            respond.println("\t\t\t<field>" + se.getData()[i] + "</field>");
        }
        if (wantTimed)
            respond.println("\t\t\t<field>" + (qbuilder.getSdf() == null ? timestampInUTC(se.getTimeStamp()) : qbuilder.getSdf().format(new Date(se.getTimeStamp()))) + "</field>");
        respond.println("\t\t</tuple>");
    }

    private long timestampInUTC(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        return cal.getTimeInMillis() + cal.getTimeZone().getOffset(cal.getTimeInMillis());
    }

    public AllowedOutputType getOt() {
        return ot;
    }
}
