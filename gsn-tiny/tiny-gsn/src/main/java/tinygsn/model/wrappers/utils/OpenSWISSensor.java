package tinygsn.model.wrappers.utils;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

import java.io.InputStream;

import tinygsn.beans.StreamElement;

public class OpenSWISSensor extends AbstractSerialProtocol{

    public static final String TAG = "OSProtocol";
    Thread readThread;

    public OpenSWISSensor(final InputStream is, OutputStream os) {
        super(is, os);
        readThread = new Thread() {
            public void run() {
                if (is != null) {
                    try {
                        int size = 0;
                        while (size == -1 ) {
                            byte[] buffer = new byte[1];
                            size = is.read(buffer);
                            Log.d(TAG, "run: " + size);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        readThread.start();
    }

    @Override
    public boolean initialize(){
        try {
            getOs().write("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum".getBytes());
            getOs().flush();
            Thread.sleep(3000);
        } catch(IOException e) {} catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public StreamElement[] getMeasurements(){
        return new StreamElement[0];
    }

}
