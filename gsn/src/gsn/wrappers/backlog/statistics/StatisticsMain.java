package gsn.wrappers.backlog.statistics;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class StatisticsMain {
	
	protected final transient Logger logger = Logger.getLogger( StatisticsMain.class );
	
	private static Map<String,DeploymentStatistics> deploymentToDeploymentStatsList = Collections.synchronizedMap(new HashMap<String,DeploymentStatistics>());
	
	private static StatisticsMain singletonObject = null;
	
	
	private StatisticsMain() {
	}
	
	
	public synchronized static DeploymentStatistics getDeploymentStatsInstance(String deploymentName) {
		if (singletonObject == null)
			singletonObject = new StatisticsMain();
		
		if (!deploymentToDeploymentStatsList.containsKey(deploymentName))
			deploymentToDeploymentStatsList.put(deploymentName, new DeploymentStatistics());
		
		return deploymentToDeploymentStatsList.get(deploymentName);
	}


	public synchronized static CoreStationStatistics getCoreStationStatsInstance(String deploymentName, String coreStationAddress) throws IOException {
		if (singletonObject == null)
			singletonObject = new StatisticsMain();
		
		if (!deploymentToDeploymentStatsList.containsKey(deploymentName))
			deploymentToDeploymentStatsList.put(deploymentName, new DeploymentStatistics());
		
		return deploymentToDeploymentStatsList.get(deploymentName).newStatisticsClass(coreStationAddress);
	}


	public synchronized static void removeCoreStationStatsInstance(String deploymentName, String coreStationAddress) throws IOException {
		if (singletonObject == null)
			singletonObject = new StatisticsMain();
		
		if (!deploymentToDeploymentStatsList.containsKey(deploymentName))
			throw new IOException("deployment " + deploymentName + " does not exist in the statistics");
		
		deploymentToDeploymentStatsList.get(deploymentName).removeCoreStationStatsInstance(coreStationAddress);
	}
}
