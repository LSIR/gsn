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
    public static final int STATE_WAITING_TIME = 2;
    public static final int STATE_WAITING_SYNC = 3;

    public int mstate = STATE_READY;

    public long max_offset = 864000;
    private long device_offset = 0L;
    private String current_modality;
    private String sensor;

    public OpenSWISSensor(WritableBT out, String sensor) {
        super(out);
        this.sensor = sensor;
    }

    @Override
    public boolean initialize(){
        return true;
    }

    @Override
    public void getMeasurements(){
        mstate = STATE_WAITING_TIME;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) { }
        getOut().write("date -u\n".getBytes());
    }

    @Override
    public void received(String s) {
        switch (mstate){
            case STATE_WAITING_TIME:
                Log.d(TAG,"at " + System.currentTimeMillis()/1000 + " current time on device is: " + s);
                s = s.trim();
                device_offset = Long.parseLong(s) - System.currentTimeMillis()/1000;
                if(Math.abs(device_offset) > max_offset){
                    mstate = STATE_WAITING_SYNC;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) { }
                    getOut().write(("date -u " + (System.currentTimeMillis() / 1000) + "\n").getBytes());
                } else {
                    mstate = STATE_WAITING_MEASUREMENT;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) { }
                    current_modality = "temperature";
                    getOut().write("temp\n".getBytes());
                }
                break;
            case STATE_WAITING_SYNC:
                Log.d(TAG,"Sync done");
                mstate = STATE_WAITING_MEASUREMENT;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { }
                current_modality = "temperature";
                getOut().write("temp\n".getBytes());
                break;
            case STATE_READY:
                Log.d(TAG, "Received: " + s);
                break;
            case STATE_WAITING_MEASUREMENT:
                Log.d(TAG, "Measurement: " + s);
                s = s.trim();
                if (Double.parseDouble(s) < 1000000000){
                    getOut().publish(new StreamElement(SerialBLEWrapper.FIELD_NAMES, SerialBLEWrapper.FIELD_TYPES, new Serializable[]{sensor, "offset", Double.parseDouble(""+device_offset)}));
                    getOut().publish(new StreamElement(SerialBLEWrapper.FIELD_NAMES, SerialBLEWrapper.FIELD_TYPES, new Serializable[]{sensor, current_modality, Double.parseDouble(s)}));
                    getOut().done();
                }
        }
    }

}
