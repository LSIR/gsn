package gsn.beans.model;

import javax.persistence.*;
import java.io.Serializable;

@Entity
public class DataChannel implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, mappedBy = "dataChannel")
    private Window window;

    @OneToOne(optional = false, mappedBy = "dataChannel")
    private Sliding sliding;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    private DataNode producer;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    private DataNode consumer;

    public Long getId() {
        return id;
    }

    public Window getWindow() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public Sliding getSliding() {
        return sliding;
    }

    public void setSliding(Sliding sliding) {
        this.sliding = sliding;
    }

    public DataNode getProducer() {
        return producer;
    }

    public void setProducer(DataNode producer) {
        this.producer = producer;
        producer.getOutChannels().add(this);
    }

    public DataNode getConsumer() {
        return consumer;
    }

    public void setConsumer(DataNode consumer) {
        this.consumer = consumer;
        consumer.getInChannels().add(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof DataChannel)) return false;

        DataChannel that = (DataChannel) other;

        if (getId() != null ? !getId().equals(that.getId()) : that.getId() != null) return false;
        if (getWindow() != null ? !getWindow().equals(that.getWindow()) : that.getWindow() != null)
            return false;
        if (getSliding() != null ? !getSliding().equals(that.getSliding()) : that.getSliding() != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = getId() != null ? getId().hashCode() : 0;
        result = 31 * result + (getWindow() != null ? getWindow().hashCode() : 0);
        result = 31 * result + (getSliding() != null ? getSliding().hashCode() : 0);
        return result;
    }
}
