package gsn.reports.beans;

import java.util.Collection;

public class Report {
	
	private String hostName;
	
	private String creationTime;
	
	private String startTime;
	
	private String endTime;
	
	private Collection<VirtualSensor> virtualSensors;
	
	public Report (String hostName, String creationTime, String startTime, String endTime, Collection<VirtualSensor> virtualSensors) {
		this.hostName = hostName;
		this.creationTime = creationTime;
		this.startTime = startTime;
		this.endTime = endTime;
		this.virtualSensors = virtualSensors;
	}

	public String getHostName() {
		return hostName;
	}

	public String getCreationTime() {
		return creationTime;
	}

	public Collection<VirtualSensor> getVirtualSensors() {
		return virtualSensors;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getEndTime() {
		return endTime;
	}
}
