package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.IOException;

public interface DeliverySystem {

	public abstract void writeStructure(DataField[] fields) throws IOException;

	public abstract boolean writeStreamElement(StreamElement se);

    public abstract boolean writeKeepAliveStreamElement();

	public abstract void close();

	public abstract boolean isClosed();

}