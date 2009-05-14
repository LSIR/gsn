package gsn.beans.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
public class VirtualSensor implements Serializable {

    public static final int DEFAULT_PRIORITY = 100;

    public static final int DEFAULT_POOL_SIZE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "_name", unique = true, nullable = false, length = 100)
    private String name;

    private float loadShedding = 0.0F;

    private int priority = DEFAULT_PRIORITY;

    private long storageHistorySize;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;

    @OneToOne(optional = false, mappedBy = "virtualSensor")
    private VirtualSensorProcessor processor;

    @OneToMany(mappedBy = "virtualSensor")
    private List<InputStream> inputStream;

    private boolean accessProtected = false;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getLoadShedding() {
        return loadShedding;
    }

    public void setLoadShedding(float loadShedding) {
        this.loadShedding = loadShedding;
    }

    public long getStorageHistorySize() {
        return storageHistorySize;
    }

    public void setStorageHistorySize(long storageHistorySize) {
        this.storageHistorySize = storageHistorySize;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isAccessProtected() {
        return accessProtected;
    }

    public void setAccessProtected(boolean accessProtected) {
        this.accessProtected = accessProtected;
    }

    public VirtualSensorProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(VirtualSensorProcessor processor) {
        this.processor = processor;
    }

    public List<InputStream> getInputStream() {
        return inputStream;
    }

    public void setInputStream(List<InputStream> inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof VirtualSensor)) return false;

        VirtualSensor that = (VirtualSensor) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        return result;
    }
}