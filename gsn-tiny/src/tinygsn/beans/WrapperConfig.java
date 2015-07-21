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
	private String param = "";
	
	
	public String getParam() {
		return param;
	}
	public void setParam(String param) {
		this.param = param;
	}
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

	public WrapperConfig(int id, String name, AbstractController controller){
		wrapperName = name;
		this.controller = controller;
	}
	
	public WrapperConfig(Parcel source){
		id = source.readInt();
		wrapperName = source.readString();
		param = source.readString();
		setController(StaticData.findController(id));
	}

	public WrapperConfig(int id, String name,
			AndroidControllerListVS globalController, String parameter) {
		wrapperName = name;
		this.controller = globalController;
		this.param = parameter;
	}
	
	public AbstractController getController() {
		return controller;
	}
	public void setController(AbstractController controller) {
		this.controller = controller;
		this.controller.setId(id);
	}
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(wrapperName);
		dest.writeString(param);
		StaticData.addController((AndroidControllerListVS) getController());
		
	}
	
	public static final Parcelable.Creator<WrapperConfig> CREATOR  = new Creator<WrapperConfig>() {

	    public WrapperConfig createFromParcel(Parcel source) {

	    	WrapperConfig cf = new WrapperConfig(source);

			return cf;
	    }

	    public WrapperConfig[] newArray(int size) {
	        return new WrapperConfig[size];
	    }

	};
	
	

}
