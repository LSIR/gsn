package tinygsn.model.wrappers.utils;

import java.io.OutputStream;

import java.io.InputStream;

import tinygsn.beans.StreamElement;

public class OpenSWISSensor extends AbstractSerialProtocol{


    public OpenSWISSensor(InputStream is, OutputStream os) {
        super(is, os);
    }

    @Override
    public boolean initialize(){
        return true;
    }

    @Override
    public StreamElement[] getMeasurements(){
        return new StreamElement[0];
    }

}
