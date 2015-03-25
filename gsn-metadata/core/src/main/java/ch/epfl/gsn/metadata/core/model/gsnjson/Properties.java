
package ch.epfl.gsn.metadata.core.model.gsnjson;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;

@Generated("org.jsonschema2pojo")
public class Properties {

    @SerializedName("vs_name")
    @Expose
    private String vsName;
    @Expose
    private List<List<String>> values = new ArrayList<List<String>>();
    @Expose
    private List<Field> fields = new ArrayList<Field>();
    @Expose
    private Stats stats;
    @Expose
    private String altitude;
    @Expose
    private String name;
    @Expose
    private String latitude;
    @Expose
    private String description;
    @Expose
    private String exposition;
    @Expose
    private String longitude;
    @Expose
    private String accessProtected;
    @Expose
    private String metadata;
    @SerializedName("gps_precision")
    @Expose
    private String gpsPrecision;
    @Expose
    private String geographical;
    @Expose
    private String slope;

    /**
     * 
     * @return
     *     The vsName
     */
    public String getVsName() {
        return vsName;
    }

    /**
     * 
     * @param vsName
     *     The vs_name
     */
    public void setVsName(String vsName) {
        this.vsName = vsName;
    }

    /**
     * 
     * @return
     *     The values
     */
    public List<List<String>> getValues() {
        return values;
    }

    /**
     * 
     * @param values
     *     The values
     */
    public void setValues(List<List<String>> values) {
        this.values = values;
    }

    /**
     * 
     * @return
     *     The fields
     */
    public List<Field> getFields() {
        return fields;
    }

    /**
     * 
     * @param fields
     *     The fields
     */
    public void setFields(List<Field> fields) {
        this.fields = fields;
    }

    /**
     * 
     * @return
     *     The stats
     */
    public Stats getStats() {
        return stats;
    }

    /**
     * 
     * @param stats
     *     The stats
     */
    public void setStats(Stats stats) {
        this.stats = stats;
    }

    /**
     * 
     * @return
     *     The altitude
     */
    public String getAltitude() {
        return altitude;
    }

    /**
     * 
     * @param altitude
     *     The altitude
     */
    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    /**
     * 
     * @return
     *     The name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @param name
     *     The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return
     *     The latitude
     */
    public String getLatitude() {
        return latitude;
    }

    /**
     * 
     * @param latitude
     *     The latitude
     */
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    /**
     * 
     * @return
     *     The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @param description
     *     The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @return
     *     The exposition
     */
    public String getExposition() {
        return exposition;
    }

    /**
     * 
     * @param exposition
     *     The exposition
     */
    public void setExposition(String exposition) {
        this.exposition = exposition;
    }

    /**
     * 
     * @return
     *     The longitude
     */
    public String getLongitude() {
        return longitude;
    }

    /**
     * 
     * @param longitude
     *     The longitude
     */
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    /**
     * 
     * @return
     *     The accessProtected
     */
    public String getAccessProtected() {
        return accessProtected;
    }

    /**
     * 
     * @param accessProtected
     *     The accessProtected
     */
    public void setAccessProtected(String accessProtected) {
        this.accessProtected = accessProtected;
    }

    /**
     * 
     * @return
     *     The metadata
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * 
     * @param metadata
     *     The metadata
     */
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    /**
     * 
     * @return
     *     The gpsPrecision
     */
    public String getGpsPrecision() {
        return gpsPrecision;
    }

    /**
     * 
     * @param gpsPrecision
     *     The gps_precision
     */
    public void setGpsPrecision(String gpsPrecision) {
        this.gpsPrecision = gpsPrecision;
    }

    /**
     * 
     * @return
     *     The geographical
     */
    public String getGeographical() {
        return geographical;
    }

    /**
     * 
     * @param geographical
     *     The geographical
     */
    public void setGeographical(String geographical) {
        this.geographical = geographical;
    }

    /**
     * 
     * @return
     *     The slope
     */
    public String getSlope() {
        return slope;
    }

    /**
     * 
     * @param slope
     *     The slope
     */
    public void setSlope(String slope) {
        this.slope = slope;
    }

}
