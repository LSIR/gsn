package tinygsn.beans;

import android.os.Parcel;
import android.os.Parcelable;

public class DeliveryRequest implements Parcelable {
	private String url;
	private String key;
	private int mode;
	private String vsname;
	private long lastTime;
	private int id;
	private boolean active;
	private long iterationTime = 30000;

	public DeliveryRequest(String url, String key, int mode, String vsname,
						   int id, long iterationTime) {
		this.url = url;
		this.key = key;
		this.mode = mode;
		this.vsname = vsname;
		this.id = id;
		this.iterationTime = iterationTime;
	}

	public DeliveryRequest(Parcel source) {
		this.id = source.readInt();
		this.url = source.readString();
		this.key = source.readString();
		this.vsname = source.readString();
		this.mode = source.readInt();
		this.lastTime = source.readLong();
		this.iterationTime = source.readLong();
		this.active = source.readString().equals("1");
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

	public long getIterationTime() {
		return iterationTime;
	}

	public void setIterationTime(long iterationTime) {
		this.iterationTime = iterationTime;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(url);
		dest.writeString(key);
		dest.writeString(vsname);
		dest.writeInt(mode);
		dest.writeLong(lastTime);
		dest.writeLong(iterationTime);
		dest.writeString(active ? "1" : "0");
	}

	public static final Parcelable.Creator<DeliveryRequest> CREATOR = new Creator<DeliveryRequest>() {

		public DeliveryRequest createFromParcel(Parcel source) {

			DeliveryRequest cf = new DeliveryRequest(source);

			return cf;
		}

		public DeliveryRequest[] newArray(int size) {
			return new DeliveryRequest[size];
		}

	};
}
