package gsn.http.datarequest;

public class AbstractQuery {

    private StringBuilder standardQuery = null;
    private LimitCriterion limitCriterion = null;

    public AbstractQuery(StringBuilder standardQuery, LimitCriterion limitCriterion) {
        this.standardQuery = standardQuery;
        this.limitCriterion = limitCriterion;
    }

    public StringBuilder getStandardQuery() {
        return standardQuery;
    }

    public void setStandardQuery(StringBuilder standardQuery) {
        this.standardQuery = standardQuery;
    }

    public LimitCriterion getLimitCriterion() {
        return limitCriterion;
    }

    public void setLimitCriterion(LimitCriterion limitCriterion) {
        this.limitCriterion = limitCriterion;
    }
}
