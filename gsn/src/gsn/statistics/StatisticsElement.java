package gsn.statistics;

public final class StatisticsElement {
	
	private String source = null;
	
	private String stream = null;
	
	private Long volume = null;
	
	private Long timestamp = null;
	
	public StatisticsElement() { }
	
	/**
	 * 
	 * @param source the source name of the sensor data.
	 * @param stream the stream name of the sensor data as specified in the virtual
	 *               sensors xml-file.
	 * @param volume in number of bytes.
	 * @param timestamp the time in milliseconds this statistics element has been
	 *                  created.
	 */
	public StatisticsElement(long timestamp, String source, String stream, long volume) {
		if (source != null)
			this.source = source.toLowerCase().trim();
		if (stream != null)
			this.stream = stream.toLowerCase().trim();
		this.volume = volume;
		this.timestamp = timestamp;
	}
	
	public String getSource() {
		return source;
	}
	
	public String getStream() {
		return stream;
	}
	
	public Long getVolume() {
		return volume;
	}
	
	public Long getProcessTime() {
		return timestamp;
	}
}
