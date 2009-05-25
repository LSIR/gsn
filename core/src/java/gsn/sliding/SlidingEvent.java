package gsn.sliding;

import gsn.beans.StreamElement;
import gsn.beans.model.Sliding;
import gsn.beans.model.Window;

import java.util.List;

public class SlidingEvent {

    private List<StreamElement> windowData;
    private StreamElement streamElement;
    private Sliding sliding;
    private Window window;

    public SlidingEvent(StreamElement slideTriggered, Sliding sliding, Window window, List<StreamElement> windowData) {
        this.windowData = windowData;
        this.window = window;
        this.sliding = sliding;
        this.streamElement = slideTriggered;
    }

    public List<StreamElement> getWindowData() {
        return windowData;
    }

    public StreamElement getStreamElement() {
        return streamElement;
    }

    public Sliding getSliding() {
        return sliding;
    }

    public Window getWindow() {
        return window;
    }
}
