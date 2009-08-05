package gsn2.sliding;

import gsn.beans.StreamElement;

public class SlidingEvent {
	private StreamElement streamElement;
  
    public SlidingEvent(StreamElement streamElement) {
        this.streamElement = streamElement;
    }

    public StreamElement getStreamElement() {
        return streamElement;
    }

}
