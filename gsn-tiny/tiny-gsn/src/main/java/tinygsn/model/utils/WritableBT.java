package tinygsn.model.utils;

import tinygsn.beans.StreamElement;

public interface WritableBT {
    public void write(byte[] b);
    public void done();
    public void publish(StreamElement se);
}
