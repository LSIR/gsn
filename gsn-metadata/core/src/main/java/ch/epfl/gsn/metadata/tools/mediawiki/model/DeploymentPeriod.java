package ch.epfl.gsn.metadata.tools.mediawiki.model;

import java.util.Date;

/**
 * Created by kryvych on 01/12/14.
 */
public class DeploymentPeriod {

    private Date from;
    private Date to;

    public DeploymentPeriod(Date from, Date to) {
        this.from = from;
        this.to = to;
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public void merge(DeploymentPeriod deploymentPeriod) {
        //ToDo: implement, expand period with new dates.
        //ToDo: logic for dealing with null and ongoing measurements
    }
}
