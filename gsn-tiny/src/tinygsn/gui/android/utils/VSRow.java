package tinygsn.gui.android.utils;

/**
 * A Virtual Sensor row in the List of VS activity  
 * @author Do Ngoc Hoan (hoan.do@epfl.ch)
 *
 */
public class VSRow {
	private String name;
	private boolean isRunning;
	private String latestValue;
	
	public VSRow(String name, boolean isRunning, String latestValues) {
		super();
		this.setName(name);
		this.setRunning(isRunning);
		this.setLatestValue(latestValues);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public void setRunning(boolean isRunning) {
		this.isRunning = isRunning;
	}

	public String getLatestValue() {
		return latestValue;
	}

	public void setLatestValue(String latestValue) {
		this.latestValue = latestValue;
	}

}
