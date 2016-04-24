package tinygsn.model.wrappers.utils;


import android.util.Log;

import com.nordicsemi.nrfUARTv2.SerialBLEWrapper;

import java.io.Serializable;

import tinygsn.beans.StreamElement;
import tinygsn.model.utils.WritableBT;

public class OpenSWISSensor extends AbstractSerialProtocol{

    public static final String TAG = "OSProtocol";

    public static final int STATE_READY = 0;
    public static final int STATE_WAITING_MEASUREMENT = 1;

    public int mstate = STATE_READY;

    public OpenSWISSensor(WritableBT out) {
        super(out);

    }

    @Override
    public boolean initialize(){
        return true;
    }

    @Override
    public void getMeasurements(){
        mstate = STATE_WAITING_MEASUREMENT;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { }
        getOut().write("temp\n".getBytes());
    }

    @Override
    public void received(String s) {
        switch (mstate){
            case STATE_READY:
                Log.d(TAG, "Received: " + s);
                break;
            case STATE_WAITING_MEASUREMENT:
                Log.d(TAG, "Measurement: " + s);
                s = s.trim();
                getOut().publish(new StreamElement(SerialBLEWrapper.FIELD_NAMES,SerialBLEWrapper.FIELD_TYPES, new Serializable[]{Double.parseDouble(s)}));
                getOut().done();
        }
    }

}
