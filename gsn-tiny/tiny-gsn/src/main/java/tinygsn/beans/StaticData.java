package tinygsn.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import android.content.Context;
import android.content.Intent;

import org.epfl.locationprivacy.adaptiveprotection.AdaptiveProtection;
import org.epfl.locationprivacy.adaptiveprotection.AdaptiveProtectionInterface;

import tinygsn.model.vsensor.AbstractVirtualSensor;
import tinygsn.model.wrappers.AbstractWrapper;
import tinygsn.model.publishers.AbstractDataPublisher;

public class StaticData {

	private static int LastIdUsed = 1;
	public static InputStream is;
	private static Map<String, Intent> runningServices = new HashMap<String, Intent>();
	private static Map<Integer, VSensorConfig> configMap = new HashMap<Integer, VSensorConfig>();
	private static AdaptiveProtectionInterface protectionInterface;
	public static Map<String, Integer> IDMap = new HashMap<String, Integer>();

	public static AdaptiveProtectionInterface getAdaptiveProtectionInterface() {
		if (protectionInterface == null) {
			protectionInterface = new AdaptiveProtection(StaticData.globalContext);
		}
		return protectionInterface;
	}

	public static void saveNameID(int id, String name) {
		IDMap.put(name, id);
	}

	public static int retrieveIDByName(String Name) {
		for (Map.Entry<String, Integer> item : IDMap.entrySet()) {
			if (item.getKey().equals(Name))
				return item.getValue();
		}
		return -1;

	}

	public static VSensorConfig findConfig(int id) {
		return configMap.get(id);
	}

	public static void addConfig(int id, VSensorConfig config) {
		configMap.put(id, config);
	}

	synchronized static public Intent getRunningIntentByName(String name) {

		for (Map.Entry<String, Intent> item : runningServices.entrySet()) {
			if (item.getKey().equals(name))
				return item.getValue();

		}
		return null;
	}

	synchronized static public void IntentStopped(String name) {
		Iterator<Entry<String, Intent>> it = runningServices.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Intent> item = (Map.Entry<String, Intent>) it.next();
			if (item.getKey().equals(name)) {
				runningServices.remove(name);
				return;
			}
		}
	}

	synchronized static public void addRunningService(String name, Intent intent) {
		runningServices.put(name, intent);
	}

	synchronized static public int findNextID() {
		return LastIdUsed++;
	}

	public static Context globalContext;

	private static Map<String, AbstractVirtualSensor> vsMap = new HashMap<>();

	public static AbstractVirtualSensor getProcessingClassByVSConfig(VSensorConfig config) throws Exception {
		if (vsMap.containsKey(config.getName())) {
			return vsMap.get(config.getName());
		}
		AbstractVirtualSensor vs = (AbstractVirtualSensor) Class.forName(config.getProcessingClassName()).newInstance();

		vs.setVirtualSensorConfiguration(config);
		vs.is = config.getInputStream();
		vs.initialize_wrapper();
		vsMap.put(config.getName(), vs);
		return vs;
	}

	public static void deleteVS(String name) {
		vsMap.remove(name);
	}

	public static AbstractVirtualSensor getProcessingClassByName(String name) {
		return vsMap.get(name);
	}

	private static Map<String, AbstractWrapper> wrapperMap = new HashMap<>();

	public static AbstractWrapper getWrapperByName(String name) throws Exception {
		if (wrapperMap.containsKey(name)) {
			wrapperMap.get(name).update_wrapper();
			return wrapperMap.get(name);
		}
		String[] realNames = name.split("\\?");
		WrapperConfig wc = null;
		if (realNames.length == 2) {
			wc = new WrapperConfig(0, name, realNames[1]);
		} else {
			wc = new WrapperConfig(0, name);
		}
		AbstractWrapper wrapper = (AbstractWrapper) Class.forName(realNames[0]).getDeclaredConstructor(new Class[]{WrapperConfig.class}).newInstance(wc);
		wrapper.updateWrapperInfo();
		wrapper.initialize_wrapper();
		wrapperMap.put(name, wrapper);

		return wrapper;
	}

	public static Map<Integer, StreamSource> sourceMap = new HashMap<Integer, StreamSource>();

	public static ArrayList<String> getLocalWrapperNames() {
		ArrayList<String> a = new ArrayList<String>();
		for (String s : wrapperMap.keySet()) {
			if (s.contains("LocalWrapper?")) {
				a.add(s);
			}
		}
		return a;
	}

}
