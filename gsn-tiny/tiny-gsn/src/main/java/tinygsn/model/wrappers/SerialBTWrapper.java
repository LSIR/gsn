/**
 * Global Sensor Networks (GSN) Source Code
 * Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
 * <p/>
 * This file is part of GSN.
 * <p/>
 * GSN is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p/>
 * GSN is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with GSN. If not, see <http://www.gnu.org/licenses/>.
 * <p/>
 * File: gsn-tiny/src/tinygsn/model/wrappers/USBplugO3Wrapper.java
 *
 * @author Do Ngoc Hoan
 */
package tinygsn.model.wrappers;

//import java.io.Serializable;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.wrappers.utils.AbstractSerialProtocol;
import tinygsn.services.WrapperService;
import tinygsn.utils.ToastUtils;


public class SerialBTWrapper extends AbstractWrapper {

	public SerialBTWrapper(WrapperConfig wc) {
		super(wc);
	}

	public SerialBTWrapper() {
	}

    public static final String TAG = "SerialBTWrapper";


	private static final String[] FIELD_NAMES = new String[]{ "humidity", "temperature"};

	private static final Byte[] FIELD_TYPES = new Byte[]{DataTypes.DOUBLE,DataTypes.DOUBLE};

	private static final String[] FIELD_DESCRIPTION = new String[]{"humidity", "temperature"};

	private static final String[] FIELD_TYPES_STRING = new String[]{"double", "double"};

	public final Class<? extends WrapperService> getSERVICE() {
		return SerialBTService.class;
	}

	private AbstractSerialProtocol sensor;

	@Override
	public void runOnce() {
		updateWrapperInfo();
		if (dcDuration > 0) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null) {
                if (!mBluetoothAdapter.isEnabled()) {
                    ToastUtils.showToastInUiThread(StaticData.globalContext, "The Bluetooth wrapper needs Bluetooth to be enabled.", Toast.LENGTH_LONG);
                } else {
                    mBluetoothAdapter.cancelDiscovery();
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        device.fetchUuidsWithSdp();
                        StreamElement[] se = new StreamElement[0];
                        for (ParcelUuid uuid : device.getUuids()) {
                            Log.d(TAG, "UUID : " + uuid.getUuid().toString());
                            if (uuid.getUuid().compareTo(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")) == 0) {
                                BluetoothSocket socket = null;
                                try {
                                    socket = device.createRfcommSocketToServiceRecord(uuid.getUuid());
                                    try {
                                        socket.connect();
                                        Log.e(TAG,"Connected");
                                    } catch (IOException e) {
                                        Log.e(TAG, e.getMessage());
                                        try {
                                            Log.e(TAG, "trying fallback...");
                                            socket = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(device, 1);
                                            socket.connect();
                                            Log.e(TAG, "Connected");
                                        } catch (Exception e2) {
                                            Log.e(TAG, "Couldn't establish Bluetooth connection!");
                                        }
                                    }
                                    if (socket.isConnected()) {
                                      //  sensor = new OpenSWISSensor(socket.getInputStream(), socket.getOutputStream());
                                      //  sensor.initialize();
                                      //  se = sensor.getMeasurements();
                                        socket.close();
                                    }
                                } catch (IOException e){
                                    Log.e(TAG, "BT connection error", e);
                                    try {
                                        socket.close();
                                    } catch (Exception e1){}
                                //} catch (InterruptedException e){
                                }
                                break;
                            }
                        }
                        for (StreamElement s : se) {
                            postStreamElement(s);
                        }
                    }
                }
            }
		}
	}




	@Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					                        FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[output.size()]);
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}


	public static class SerialBTService extends WrapperService {

		public SerialBTService() {
			super("serialBTService");

		}
	}

}