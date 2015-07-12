package tinygsn.beans;

import java.util.List;
import java.util.Properties;
import java.util.Vector;

import tinygsn.controller.AbstractController;
import tinygsn.controller.AndroidControllerListVS;
import android.os.Parcel;
import android.os.Parcelable;

public class WrapperConfig implements Parcelable {
	
	private String wrapperName;
	private List<Properties> predicates = new Vector<Properties>();
	private int id;
	private AbstractController controller = null;
	private boolean running = true;
	
	
	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public String getWrapperName() {
		return wrapperName;
	}
	
	public void setWrapperName(String wrapperName) {
		this.wrapperName = wrapperName;
	}

	public WrapperConfig(int id, String name){
		wrapperName = name;
	}
	
	public WrapperConfig(Parcel source){
		id = source.readInt();
		wrapperName = source.readString();
		setController(StaticData.findController(id));
	}

	public AbstractController getController() {
		return controller;
	}
	public void setController(AbstractController controller) {
		this.controller = controller;
	}
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(wrapperName);
		StaticData.addController((AndroidControllerListVS) getController());
		
	}
	
	

}
