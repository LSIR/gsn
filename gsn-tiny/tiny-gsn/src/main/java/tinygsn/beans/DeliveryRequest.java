package tinygsn.beans;

public class DeliveryRequest {
	private String url;
	private String key;
	private int mode;
	private String vsname;
	private long lastTime;
	private int id;
	private boolean active;
	
	public DeliveryRequest(String url, String key, int mode, String vsname,
			int id) {
		this.url = url;
		this.key = key;
		this.mode = mode;
		this.vsname = vsname;
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getMode() {
		return mode;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public String getVsname() {
		return vsname;
	}

	public void setVsname(String vsname) {
		this.vsname = vsname;
	}

	public long getLastTime() {
		return lastTime;
	}

	public void setLastTime(long lastTime) {
		this.lastTime = lastTime;
	}
	

}
