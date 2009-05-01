package gsn.http.datarequest;

import gsn.Main;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class QueriesBuilder {

    private static transient Logger logger = Logger.getLogger(QueriesBuilder.class);

    /* Mandatory Parameters */
    public static final String PARAM_VSNAMES_AND_FIELDS = "vsname";

    /* Optional Parameters */
    public static final String PARAM_AGGREGATE_CRITERIA = "groupby";
    public static final String PARAM_STANDARD_CRITERIA = "critfield";
    public static final String PARAM_MAX_NB = "nb";
    public static final String PARAM_TIME_FORMAT = "timeformat";

    /* Parsed Parameters */
    private HashMap<String, FieldsCollection> vsnamesAndStreams = null;
    private AggregationCriterion aggregationCriterion = null;
    private ArrayList<StandardCriterion> standardCriteria = null;
    private LimitCriterion limitCriterion = null;

    private Hashtable<String, AbstractQuery> sqlQueries;

    protected Map<String, String[]> requestParameters;

    protected SimpleDateFormat sdf = new SimpleDateFormat(Main.getContainerConfig().getTimeFormat());

    private static Hashtable<String, String> allowedTimeFormats = null;

    static {
        allowedTimeFormats = new Hashtable<String, String>();
        allowedTimeFormats.put("iso", "yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        allowedTimeFormats.put("unix", "unix");
    }

    public QueriesBuilder(Map<String, String[]> requestParameters) throws DataRequestException {
        this.requestParameters = requestParameters;
        parseParameters();
        buildSQLQueries();
    }

    private void parseParameters() throws DataRequestException {

        String[] vsnamesParameters = requestParameters.get(PARAM_VSNAMES_AND_FIELDS);

        if (vsnamesParameters == null)
            throw new DataRequestException("You must specify at least one >" + PARAM_VSNAMES_AND_FIELDS + "< parameter.");

        vsnamesAndStreams = new HashMap<String, FieldsCollection>();
        String name;
        String[] streams;
        for (int i = 0; i < vsnamesParameters.length; i++) {
            int firstColumnIndex = vsnamesParameters[i].indexOf(':');
            if (firstColumnIndex == -1) {
                name = vsnamesParameters[i];
                streams = new String[0];
            } else {
                name = vsnamesParameters[i].substring(0, firstColumnIndex);
                streams = vsnamesParameters[i].substring(firstColumnIndex + 1).split(":");
            }

            vsnamesAndStreams.put(name, new FieldsCollection(streams));
        }

        String ac = getParameter(requestParameters, PARAM_AGGREGATE_CRITERIA);
        if (ac != null) {
            aggregationCriterion = new AggregationCriterion(ac);
        }

        String[] cc = requestParameters.get(PARAM_STANDARD_CRITERIA);
        if (cc != null) {
            standardCriteria = new ArrayList<StandardCriterion>();
            for (int i = 0; i < cc.length; i++) {
                standardCriteria.add(new StandardCriterion(cc[i]));
            }
        }

        String lm = getParameter(requestParameters, PARAM_MAX_NB);
        if (lm != null) limitCriterion = new LimitCriterion(lm);

        String timeformat = getParameter(requestParameters, PARAM_TIME_FORMAT);
        if (timeformat != null && allowedTimeFormats.containsKey(timeformat)) {
            String format = allowedTimeFormats.get(timeformat);
            sdf = format.compareToIgnoreCase("unix") == 0 ? null : new SimpleDateFormat(format);
        }
    }

    public Hashtable<String, AbstractQuery> getSqlQueries() {
        return sqlQueries;
    }

    private void buildSQLQueries() {

        this.sqlQueries = new Hashtable<String, AbstractQuery>();

        // Fields and Virtual Sensors
        Iterator<Entry<String, FieldsCollection>> iter = vsnamesAndStreams.entrySet().iterator();
        Entry<String, FieldsCollection> next;
        String[] fields;
        String vsname;
        StringBuilder partFields;
        //StringBuilder partVS;
        StringBuilder sqlQuery;
        while (iter.hasNext()) {

            next = iter.next();
            fields = next.getValue().getFields();
            vsname = next.getKey();

            // Standard Criteria
            StringBuilder partStandardCriteria = new StringBuilder();
            if (standardCriteria != null) {
                StandardCriterion lastStandardCriterionLinkedToVs = null;
                StandardCriterion cc;
                for (int i = 0; i < standardCriteria.size(); i++) {
                    cc = standardCriteria.get(i);
                    if (cc.getVsname().compareTo("") == 0 || cc.getVsname().compareToIgnoreCase(vsname) == 0) {

                        if (lastStandardCriterionLinkedToVs != null) {
                            partStandardCriteria.append(lastStandardCriterionLinkedToVs.getCritJoin() + " " + cc.getNegation() + " " + cc.getField() + " " + cc.getOperator() + " ");
                        } else {
                            partStandardCriteria.append(cc.getNegation() + " " + cc.getField() + " " + cc.getOperator() + " ");
                        }

                        lastStandardCriterionLinkedToVs = cc;

                        if (cc.getOperator().compareToIgnoreCase("like") == 0) partStandardCriteria.append("'%");

                        partStandardCriteria.append(cc.getValue());

                        if (cc.getOperator().compareToIgnoreCase("like") == 0) partStandardCriteria.append("%'");
                        partStandardCriteria.append(" ");
                    }
                }
                if (lastStandardCriterionLinkedToVs != null) partStandardCriteria.insert(0, "where ");
            }

            partFields = new StringBuilder();
            for (int i = 0; i < fields.length; i++) {
                if (partFields.length() > 0)
                    partFields.append(", ");
                if (aggregationCriterion != null) partFields.append(aggregationCriterion.getGroupOperator() + "(");
                partFields.append(fields[i]);
                if (aggregationCriterion != null) partFields.append(") as " + fields[i]);

            }

            if (aggregationCriterion != null) {
                if (partFields.length() > 0) {
                    partFields.append(", ");
                }
                partFields.append("floor(timed/" + aggregationCriterion.getTimeRange() + ") as aggregation_interval ");
            } else partFields.append(" ");


            // Build a final query
            sqlQuery = new StringBuilder();
            sqlQuery.append("select ");
            sqlQuery.append(partFields);
            sqlQuery.append("from ").append(vsname).append(" ");
            sqlQuery.append(partStandardCriteria);
            if (aggregationCriterion == null) sqlQuery.append("order by timed desc ");
            else sqlQuery.append("group by aggregation_interval desc ");

            logger.debug("SQL Query built >" + sqlQuery.toString() + "<");

            this.sqlQueries.put(vsname, new AbstractQuery(sqlQuery, limitCriterion));
        }
    }

    public AggregationCriterion getAggregationCriterion() {
        return aggregationCriterion;
    }

    public ArrayList<StandardCriterion> getStandardCriteria() {
        return standardCriteria;
    }

    public HashMap<String, FieldsCollection> getVsnamesAndStreams() {
        return vsnamesAndStreams;
    }

    public LimitCriterion getLimitCriterion() {
        return limitCriterion;
    }


    /**
     * Returns the value of a request parameter as a <code>String</code>, or <code>null</code> if the parameter does not exist.
     */
    protected static String getParameter(Map<String, String[]> parameters, String requestedParameter) {
        String[] rpv = parameters.get(requestedParameter);
        return (rpv == null || rpv.length == 0) ? null : rpv[0];
    }

    public SimpleDateFormat getSdf() {
        return sdf;
    }


}
