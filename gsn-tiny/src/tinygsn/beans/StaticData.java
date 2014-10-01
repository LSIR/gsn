package tinygsn.beans;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Intent;

import tinygsn.controller.AndroidControllerListVSNew;

public class StaticData {
	public static final int USER_ID = 123;
	private static int LastIdUsed = 1;
	public static InputStream is;
	private static Map<Integer, AndroidControllerListVSNew> controllerMap = new HashMap< Integer, AndroidControllerListVSNew>();
	private static Map<String, Intent> runningServices = new HashMap<String, Intent>();
	private static Map<Integer, VSensorConfig> configMap = new HashMap<Integer, VSensorConfig>();
	public static Map<String, String> vsNames = new HashMap<String, String>();
	public static Map<String, Integer> IDMap = new HashMap<String, Integer>();
	
	public static void saveNameID(int id, String name)
	{
		IDMap.put(name, id);
	}
	public static int retrieveIDByName(String Name)
	{
		for(Map.Entry<String, Integer> item: IDMap.entrySet())
		{
			if(item.getKey().equals(Name))
				return item.getValue();
		}
		return -1;
	
	}
	public static void saveName(String vsName, String vsType)
	{
		vsNames.put(vsType, vsName);
	}
	
	public static String findNameVs(String type)
	{
		for(Map.Entry<String, String> item: vsNames.entrySet())
		{
			if(item.getKey().equals(type))
				return item.getValue();
		}
		return null;
	}
	public static VSensorConfig findConfig(int id)
	{
		return configMap.get(id);
	}
	public static void addConfig(int id, VSensorConfig config)
	{
		configMap.put(id, config);
	}

	synchronized static public Intent getRunningIntentByName(String name)
	{
			
		for(Map.Entry<String, Intent> item: runningServices.entrySet())
		{
			if(item.getKey().equals(name))
				return item.getValue();

		}
		return null;
	}
	synchronized static public void IntentStopped(String name)
	{
		Iterator it = runningServices.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry<String, Intent> item = (Map.Entry<String, Intent>) it.next();
			if(item.getKey().equals(name))
			{
				runningServices.remove(name);
				return;
			}
		}
	}
	synchronized static public void addRunningService(String name, Intent intent)
	{
		runningServices.put(name, intent);
	}
	
	synchronized static public void addController(AndroidControllerListVSNew controller)
	{
		controllerMap.put(controller.getId(), controller);
	}
	synchronized static public AndroidControllerListVSNew findController(int id)
	{
		return controllerMap.get(id);
	}
	synchronized static public int findNextID()
	{
		return LastIdUsed++;
	}
	
}
