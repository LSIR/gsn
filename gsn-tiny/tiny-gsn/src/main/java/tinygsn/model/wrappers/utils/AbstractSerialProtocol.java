package tinygsn.model.wrappers.utils;

import tinygsn.beans.StreamElement;
import tinygsn.model.utils.WritableBT;

public class AbstractSerialProtocol {

    private WritableBT out = null;

    public AbstractSerialProtocol(WritableBT out) {
      this.out = out;
    }

    public WritableBT getOut() {
        return out;
    }

    public void setOut(WritableBT out) {
        this.out = out;
    }

    public boolean initialize(){
        return true;
    }

    public void received(String s) {}

    public void getMeasurements(){
    }


}
