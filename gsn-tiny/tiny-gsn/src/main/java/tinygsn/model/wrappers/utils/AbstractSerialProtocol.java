package tinygsn.model.wrappers.utils;


import java.io.OutputStream;

import java.io.InputStream;
import tinygsn.beans.StreamElement;

public class AbstractSerialProtocol {

    private InputStream is = null;
    private OutputStream os = null;

    public AbstractSerialProtocol(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
    }

    public InputStream getIs() {
        return is;
    }

    public void setIs(InputStream is) {
        this.is = is;
    }

    public OutputStream getOs() {
        return os;
    }

    public void setOs(OutputStream os) {
        this.os = os;
    }

    public boolean initialize(){
        return true;
    }

    public StreamElement[] getMeasurements(){
        return new StreamElement[0];
    }


}
