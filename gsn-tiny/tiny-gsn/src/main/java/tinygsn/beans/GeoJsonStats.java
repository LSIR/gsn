package tinygsn.beans;

import com.google.gson.annotations.SerializedName;

public class GeoJsonStats {
    public Long getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(Long startDatetime) {
        this.startDatetime = startDatetime;
    }

    public Long getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(Long endDatetime) {
        this.endDatetime = endDatetime;
    }

    @SerializedName("start-datetime") private Long startDatetime;
    @SerializedName("end-datetime") private Long endDatetime;
}
