package gsn.beans;

import java.util.ArrayList;

public abstract class DataProvider {
    private BetterQueue queue = new BetterQueue();
    private String query;
    private String windowSize;
    private String slidingValue;
    private ArrayList<DataProvider> children = new ArrayList<DataProvider>();
    private DataProvider parent;
    private String id;

    public DataProvider getParent() {
        return parent;
    }

    public void setParent(DataProvider parent) {
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<DataProvider> getChildren() {
        return children;
    }

    public void addChild(DataProvider child) {
        this.addChild(child);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(String windowSize) {
        this.windowSize = windowSize;
    }

    public String getSlidingValue() {
        return slidingValue;
    }

    public void setSlidingValue(String slidingValue) {
        this.slidingValue = slidingValue;
    }

    public boolean isLeaf() {
        return getChildren() == null || getChildren().isEmpty();
    }
}
