/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/http/datarequest/DownloadData.java
*
* @author Ali Salehi
* @author Mehdi Riahi
* @author Timotee Maret
* @author Milos Stojanovic
*
*/

package gsn.http.datarequest;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.beans.DataField;
import gsn.http.MultiDataDownload;
import gsn.storage.DataEnumerator;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.*;

import org.apache.commons.collections.KeyValue;
import org.apache.commons.lang.StringEscapeUtils;
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
                de = Main.getStorage(nextSqlQuery.getKey()).streamedExecuteQuery(nextSqlQuery.getValue(), true, connection);

                //get units in hash map
                Iterator< VSensorConfig > vsIterator = Mappings.getAllVSensorConfigs();
                HashMap<String, String> fieldToUnitMap = new HashMap<String, String>();
                VSensorConfig sensorConfig = null;
                while ( vsIterator.hasNext( ) ) {
                    VSensorConfig senConfig = vsIterator.next( );
                    if (nextSqlQuery.getKey().equalsIgnoreCase(senConfig.getName())){
                        sensorConfig = senConfig;
                        DataField[] dataFieldArray = senConfig.getOutputStructure();
                        for (DataField df: dataFieldArray){
                            String unit = df.getUnit();
                            if (unit == null || unit.trim().length() == 0)
                                unit = "";

                            fieldToUnitMap.put(df.getName().toLowerCase(), unit);
                        }
                        break;
                    }
                }

                logger.debug("Data Enumerator: " + de);
                if (ot == AllowedOutputType.csv) {
                    respond.println("# vsname:" + nextSqlQuery.getKey());
                    respond.println("# query:" + nextSqlQuery.getValue().getStandardQuery() + (nextSqlQuery.getValue().getLimitCriterion() == null ? "" : "(" + nextSqlQuery.getValue().getLimitCriterion() + ")"));
                    for ( KeyValue df : sensorConfig.getAddressing()){
                        respond.println("# " + df.getKey().toString().toLowerCase() + ":" + df.getValue().toString());
                    }
                    respond.println("# description:" + sensorConfig.getDescription());
                } else if (ot == AllowedOutputType.xml) {
                    respond.println("\t<!-- " + nextSqlQuery.getValue().getStandardQuery() + " -->");
                    for ( KeyValue df : sensorConfig.getAddressing()){
                        respond.println("\t<!-- " + StringEscapeUtils.escapeXml(df.getKey().toString().toLowerCase()) + ":" + StringEscapeUtils.escapeXml(df.getValue().toString()) + " -->");
                    }
                    respond.println("\t<!-- description:" + StringEscapeUtils.escapeXml(sensorConfig.getDescription()) + " -->");
                    respond.println("\t<data vsname=\"" + nextSqlQuery.getKey() + "\">");
                 }

                FieldsCollection fc = qbuilder.getVsnamesAndStreams().get(nextSqlQuery.getKey());
                boolean wantTimed = true;
                boolean firstLine = true;
                LinkedList<StreamElement> streamElements = new LinkedList<StreamElement>();
                while (de.hasMoreElements()) {
                    streamElements.add(de.nextElement());
                }
                while (!streamElements.isEmpty()) {
                    if (ot == AllowedOutputType.csv) {
                        formatCSVElement(respond, streamElements.removeLast(), wantTimed, csvDelimiter, firstLine, fieldToUnitMap);
                    } else if (ot == AllowedOutputType.xml) {
                        formatXMLElement(respond, streamElements.removeLast(), wantTimed, firstLine, fieldToUnitMap);
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


    private void formatCSVElement(PrintWriter respond, StreamElement se, boolean wantTimed, String cvsDelimiter, boolean firstLine, HashMap<String, String> fieldToUnitMap) {
        if (firstLine) {
            //names of vs fields (first line)
            respond.print("# ");
            if (wantTimed)
                respond.print("time");
            for (int i = 0; i < se.getData().length; i++) {
                respond.print(cvsDelimiter);
                respond.print(se.getFieldNames()[i]);
            }
            respond.println();

            //units (second line)
            respond.print("# ");
            if (wantTimed)
                respond.print("");
            for (int i = 0; i < se.getData().length; i++) {
                respond.print(cvsDelimiter);
                respond.print(fieldToUnitMap.get(se.getFieldNames()[i].toLowerCase()));
            }
            respond.println();
        }
        if (wantTimed) {
            respond.print(qbuilder.getSdf() == null ? timestampInUTC(se.getTimeStamp()) : qbuilder.getSdf().format(new Date(se.getTimeStamp())));
        }
        for (int i = 0; i < se.getData().length; i++) {
            respond.print(cvsDelimiter);
            respond.print(se.getData()[i]);
        }
        respond.println();
    }

    private void formatXMLElement(PrintWriter respond, StreamElement se, boolean wantTimed, boolean firstLine, HashMap<String, String> fieldToUnitMap) {
        if (firstLine) {
            respond.println("\t\t<header>");
            if (wantTimed)
                respond.println("\t\t\t<field unit=\"\">time</field>");
            for (int i = 0; i < se.getData().length; i++) {
                respond.print("\t\t\t<field unit=\"" + fieldToUnitMap.get(se.getFieldNames()[i].toLowerCase()));
                respond.println("\">"+se.getFieldNames()[i]+"</field>");
            }
            respond.println("\t\t</header>");
        }
        respond.println("\t\t<tuple>");
        if (wantTimed)
            respond.println("\t\t\t<field>" + (qbuilder.getSdf() == null ? timestampInUTC(se.getTimeStamp()) : qbuilder.getSdf().format(new Date(se.getTimeStamp()))) + "</field>");
        for (int i = 0; i < se.getData().length; i++) {
            respond.println("\t\t\t<field>" + se.getData()[i] + "</field>");
        }
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
