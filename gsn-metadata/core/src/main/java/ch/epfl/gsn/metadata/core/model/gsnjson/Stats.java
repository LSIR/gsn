
package ch.epfl.gsn.metadata.core.model.gsnjson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class Stats {

    @SerializedName("start-datetime")
    @Expose
    private Long startDatetime;
    @SerializedName("end-datetime")
    @Expose
    private Long endDatetime;

    /**
     * 
     * @return
     *     The startDatetime
     */
    public Long getStartDatetime() {
        return startDatetime;
    }

    /**
     * 
     * @param startDatetime
     *     The start-datetime
     */
    public void setStartDatetime(Long startDatetime) {
        this.startDatetime = startDatetime;
    }

    /**
     * 
     * @return
     *     The endDatetime
     */
    public Long getEndDatetime() {
        return endDatetime;
    }

    /**
     * 
     * @param endDatetime
     *     The end-datetime
     */
    public void setEndDatetime(Long endDatetime) {
        this.endDatetime = endDatetime;
    }

}
