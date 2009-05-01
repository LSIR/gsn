package gsn.http.datarequest;

import java.util.Hashtable;

public class AggregationCriterion extends AbstractCriterion {

    private static Hashtable<String, String> allowedGroupOperator = null;

    static {
        allowedGroupOperator = new Hashtable<String, String>();
        allowedGroupOperator.put("max", "max");
        allowedGroupOperator.put("min", "min");
        allowedGroupOperator.put("avg", "avg");
    }

    private String critTimeRange = null;
    private String critGroupOperator = null;

    /**
     * <p>
     * Create a new Aggregation Criteria from a serialized Aggregation description.
     * The description must follow the syntax:<br />
     * <code><timerange>:<groupoperator></code>
     * </p>
     *
     * @param inlinecrits
     */
    public AggregationCriterion(String inlinecrits) throws DataRequestException {

        String[] crits = inlinecrits.split(":");

        if (crits.length != 2) throw new DataRequestException(GENERAL_ERROR_MSG + " >" + inlinecrits + "<.");

        critTimeRange = crits[0];
        critGroupOperator = getCriterion(crits[1], allowedGroupOperator);
    }

    public String toString() {
        return "Select: " + critGroupOperator.toUpperCase() + ", group by: timed/" + critTimeRange + " (" + formatTimePeriod(Long.parseLong(critTimeRange)) + ")";
    }

    public String getTimeRange() {
        return critTimeRange;
    }

    public String getGroupOperator() {
        return critGroupOperator;
    }

    public static String formatTimePeriod(long timestamp) {
        if (timestamp < 1000) return timestamp + " ms";
        if (timestamp < 60 * 1000) return (timestamp / 1000) + " sec";
        if (timestamp < 60 * 60 * 1000) return (timestamp / (1000 * 60)) + " min";
        if (timestamp < 24 * 60 * 60 * 1000) return (timestamp / (1000 * 60 * 60)) + " h";
        return (timestamp / (24 * 1000 * 60 * 60)) + " day";
	}
}