package gsn.statistics;

public interface StatisticsListener {
	
	/**
	 * This function has to return the name of the listener.
	 * 
	 * @return the listener's name
	 */
	public String listenerName();
	
	
	/**
	 * This function will get called on an statistics input event.
	 * 
	 * @param producerVS the name of the producing virtual sensor.
	 * @param statisticsElement the statistics element
	 * 
	 * @return should return true if the event has been handled properly.
	 */
	public boolean inputEvent(String producerVS, StatisticsElement statisticsElement);
	
	
	/**
	 * This function will get called on an statistics output event.
	 * 
	 * @param producerVS the name of the producing virtual sensor.
	 * @param statisticsElement the statistics element
	 * 
	 * @return should return true if the event has been handled properly.
	 */
	public boolean outputEvent(String producerVS, StatisticsElement statisticsElement);
}
