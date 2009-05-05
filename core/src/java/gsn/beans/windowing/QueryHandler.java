package gsn.beans.windowing;

import gsn.beans.StreamElement;
import gsn.beans.StreamSource;

public abstract class QueryHandler {
    protected StreamSource streamSource;

    public QueryHandler() {

    }

    public QueryHandler(StreamSource streamSource) {
        setStreamSource(streamSource);
    }

    public abstract boolean initialize();

    public abstract StringBuilder rewrite(String query);

    public abstract void finilize();

    public abstract boolean dataAvailable(StreamElement streamElement);

    public StreamSource getStreamSource() {
        return streamSource;
    }

    public void setStreamSource(StreamSource streamSource) {
        this.streamSource = streamSource;
        streamSource.setQueryHandler(this);
    }


}
