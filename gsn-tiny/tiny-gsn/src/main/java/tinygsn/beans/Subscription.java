package tinygsn.beans;

public class Subscription {


	private String url;
    private String username="guest";
    private String password="guest";
	private int mode;
	private String vsname;
	private long lastTime;
	private int id;
	private boolean active;
	private long iterationTime = 30000;

    public Subscription(String url, int mode, String vsname, int id, long iterationTime) {
		this.url = url;
		this.mode = mode;
		this.vsname = vsname;
		this.id = id;
		this.iterationTime = iterationTime;
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

    public long getIterationTime() { return iterationTime; }

    public void setIterationTime(long iterationTime) { this.iterationTime = iterationTime; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }

    public void setPassword(String password) { this.password = password; }

}
