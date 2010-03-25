package gsn.http.datarequest;

import gsn.beans.StreamElement;
import gsn.http.MultiDataDownload;
import gsn.storage.DataEnumerator;
import gsn.storage.StorageManager;

import java.io.ByteArrayOutputStream;
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

    public DownloadData(Map<String, String[]> requestParameters) throws DataRequestException {
        super(requestParameters);
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

    public String outputResult() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        outputResult(baos);
        return baos.toString();
    }

    @Override
    public void outputResult(OutputStream os) {
        PrintWriter respond = new PrintWriter(os);
        Iterator<Entry<String, AbstractQuery>> iter = qbuilder.getSqlQueries().entrySet().iterator();
        Entry<String, AbstractQuery> nextSqlQuery;
        DataEnumerator de = null;
        //StringBuilder sb = new StringBuilder();
        try {
            if (ot == AllowedOutputType.xml) {
                respond.println("<result>");
//                sb.append("<result>\n");
            }
            while (iter.hasNext()) {
                nextSqlQuery = iter.next();
                Connection connection = null;

                connection = StorageManager.getInstance().getConnection();
                de = StorageManager.getInstance().executeQuery(nextSqlQuery.getValue(), false, connection);
                //de = StorageManager.getInstance().streamedExecuteQuery(nextSqlQuery.getValue(), false, connection);
                logger.debug("Data Enumerator: " + de);
                if (ot == AllowedOutputType.csv) {
                    respond.println("##vsname:" + nextSqlQuery.getKey());
                    //sb.append("##vsname:").append(nextSqlQuery.getKey()).append("\n");
                    respond.println("##query:" + nextSqlQuery.getValue().getStandardQuery() + (nextSqlQuery.getValue().getLimitCriterion() == null ? "" : "(" + nextSqlQuery.getValue().getLimitCriterion() + ")"));
                    //sb.append("##query:").append(nextSqlQuery.getValue().getStandardQuery());
                    //if (nextSqlQuery.getValue().getLimitCriterion() != null)
                        //sb.append("(").append(nextSqlQuery.getValue().getLimitCriterion()).append(")");
                    //sb.append("\n");
                    respond.println();
                } else if (ot == AllowedOutputType.xml) {
                    respond.println("\t<!-- " + nextSqlQuery.getValue().getStandardQuery() + " -->");
                    //sb.append("\t<!-- ").append(nextSqlQuery.getValue().getStandardQuery()).append(" -->").append("\n");
                    respond.println("\t<data vsname=\"" + nextSqlQuery.getKey() + "\">");
                    //sb.append("\t<data vsname=\"").append(nextSqlQuery.getKey()).append("\">").append("\n");
                }
                FieldsCollection fc = qbuilder.getVsnamesAndStreams().get(nextSqlQuery.getKey());
                //boolean wantTimed = fc != null ? fc.isWantTimed() : false;
                boolean wantTimed = true;
                boolean firstLine = true;
                while (de.hasMoreElements()) {
                    if (ot == AllowedOutputType.csv) {
                        formatCSVElement(respond, de.nextElement(), wantTimed, csvDelimiter, firstLine);
                    } else if (ot == AllowedOutputType.xml) {
                        formatXMLElement(respond, de.nextElement(), wantTimed, firstLine);
                    }
                    firstLine = false;

                    // Flush buffer
                    //respond.print(sb.toString());
                    //respond.flush();
                    //sb.delete(0, sb.length());
                    //sb.trimToSize();
                }
                if (ot == AllowedOutputType.xml)
                    //sb.append("\t</data>").append("\n");
                    respond.println("\t</data>");
            }
            if (ot == AllowedOutputType.xml) {
                //sb.append("</result>").append("\n");
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


    private void formatCSVElement(PrintWriter respond, StreamElement se, boolean wantTimed, String cvsDelimiter, boolean firstLine) {
        if (firstLine) {
            respond.print("#");
            //sb.append("#");
            for (int i = 0; i < se.getData().length; i++) {
                respond.print(se.getFieldNames()[i]);
                //sb.append(se.getFieldNames()[i]);
                if (i != se.getData().length - 1)
                    //sb.append(cvsDelimiter);
                    respond.print(cvsDelimiter);
            }
            if (wantTimed && se.getData().length != 0)
                    //sb.append(cvsDelimiter);
                respond.print(cvsDelimiter);
            if (wantTimed)
                    //sb.append("timed");
                respond.print("timed");
            respond.println();
            //sb.append("\n");
        }
        for (int i = 0; i < se.getData().length; i++) {
            //sb.append(se.getData()[i]);
            respond.print(se.getData()[i]);
            if (i != se.getData().length - 1)
                //sb.append(cvsDelimiter);
                respond.print(cvsDelimiter);
        }
        if (wantTimed) {
            if (se.getData().length != 0)
                    //sb.append(cvsDelimiter);
                respond.print(cvsDelimiter);
//            if (qbuilder.getSdf() == null)
//                sb.append(timestampInUTC(se.getTimeStamp()));
//            else
//                sb.append(qbuilder.getSdf().format(new Date(se.getTimeStamp())));
            respond.print(qbuilder.getSdf() == null ? timestampInUTC(se.getTimeStamp()) : qbuilder.getSdf().format(new Date(se.getTimeStamp())));
        }
        //sb.append("\n");
        respond.println();
    }

    private void formatXMLElement(PrintWriter respond, StreamElement se, boolean wantTimed, boolean firstLine) {
        if (firstLine) {
            //sb.append("\t\t<header>").append("\n");
            respond.println("\t\t<header>");
            for (int i = 0; i < se.getData().length; i++) {
                 //sb.append("\t\t\t<field>").append(se.getFieldNames()[i]).append("</field>").append("\n");
                respond.println("\t\t\t<field>" + se.getFieldNames()[i] + "</field>");
            }
            if (wantTimed)
                    //sb.append("\t\t\t<field>timed</field>").append("\n");
                respond.println("\t\t\t<field>timed</field>");
            //sb.append("\t\t</header>").append("\n");
            respond.println("\t\t</header>");
        }
        //sb.append("\t\t<tuple>").append("\n");
        respond.println("\t\t<tuple>");
        for (int i = 0; i < se.getData().length; i++) {
            //sb.append("\t\t\t<field>").append(se.getData()[i]).append("</field>").append("\n");
            respond.println("\t\t\t<field>" + se.getData()[i] + "</field>");
        }
        if (wantTimed)
                //sb.append("\t\t\t<field>").append((qbuilder.getSdf() == null ? timestampInUTC(se.getTimeStamp()) : qbuilder.getSdf().format(new Date(se.getTimeStamp())))).append("</field>").append("\n");
            respond.println("\t\t\t<field>" + (qbuilder.getSdf() == null ? timestampInUTC(se.getTimeStamp()) : qbuilder.getSdf().format(new Date(se.getTimeStamp()))) + "</field>");
        //sb.append("\t\t</tuple>").append("\n");
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
