package tinygsn.beans;

import android.os.Parcel;
import android.os.Parcelable;

public class WrapperConfig implements Parcelable {
	
	private String wrapperName;
	private int id;
	private boolean running = false;
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

	public WrapperConfig(int id, String name){
		wrapperName = name;
	}
	
	public WrapperConfig(Parcel source){
		id = source.readInt();
		wrapperName = source.readString();
		param = source.readString();
		running = source.readString().equals("1");
	}

	public WrapperConfig(int id, String name, String parameter) {
		wrapperName = name;
		this.param = parameter;
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
		dest.writeString(running?"1":"0");
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
