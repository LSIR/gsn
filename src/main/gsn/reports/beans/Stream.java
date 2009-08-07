package gsn.reports.beans;

import java.util.Collection;

public class Stream {

	private String streamName;
	

	private Collection<Data> datas;

	public Stream (String streamName, Collection<Data> datas) {
		this.streamName = streamName;
		this.datas = datas;
	}

	public String getStreamName() {
		return streamName;
	}

	public Collection<Data> getDatas() {
		return datas;
	}
}
