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

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


import tinygsn.beans.DataField;
import tinygsn.beans.DataTypes;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import tinygsn.beans.WrapperConfig;
import tinygsn.model.wrappers.utils.AbstractSerialProtocol;
import tinygsn.model.wrappers.utils.OpenSWISSensor;
import tinygsn.services.WrapperService;
import tinygsn.utils.ToastUtils;

@TargetApi(18)
public class SerialBLEWrapper extends AbstractWrapper {

	public SerialBLEWrapper(WrapperConfig wc) {
		super(wc);
	}

	public SerialBLEWrapper() {
	}

    public static final String TAG = "SerialBTWrapper";

    /* ISSC Proprietary */
    public final static  UUID SERVICE_ISSC_PROPRIETARY  = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    public final static  UUID CHR_CONNECTION_PARAMETER  = UUID.fromString("49535343-6DAA-4D02-ABF6-19569ACA69FE");
    public final static  UUID CHR_AIR_PATCH             = UUID.fromString("49535343-ACA3-481C-91EC-D85E28A60318");
    public final static  UUID CHR_ISSC_TRANS_TX         = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    public final static  UUID CHR_ISSC_TRANS_RX         = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    public final static  UUID CHR_ISSC_MP               = UUID.fromString("49535343-026E-3A9B-954C-97DAEF17E26E");
    /* Client Characteristic Configuration Descriptor */
    public final static UUID DES_CLIENT_CHR_CONFIG      = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

	private static final String[] FIELD_NAMES = new String[]{ "humidity", "temperature"};

	private static final Byte[] FIELD_TYPES = new Byte[]{DataTypes.DOUBLE,DataTypes.DOUBLE};

	private static final String[] FIELD_DESCRIPTION = new String[]{"humidity", "temperature"};

	private static final String[] FIELD_TYPES_STRING = new String[]{"double", "double"};

	public final Class<? extends WrapperService> getSERVICE() {
		return SerialBLEService.class;
	}

	private AbstractSerialProtocol sensor;

    private ArrayList<BluetoothDevice> mLeDeviceListAdapter;

    private BluetoothGattCharacteristic mTransRx;

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                            mLeDeviceListAdapter.add(device);
                            Log.i(TAG, "Found device: " + device.getAddress());
                        }
                };

    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                gatt.discoverServices());
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status){
                   if (status == BluetoothGatt.GATT_SUCCESS){

                       displayGattServices(gatt.getServices());
                       BluetoothGattService proprietary = gatt.getService(SERVICE_ISSC_PROPRIETARY);
                       if (proprietary != null) {
                           BluetoothGattCharacteristic mTransTx = proprietary.getCharacteristic(CHR_ISSC_TRANS_TX);
                           mTransRx = proprietary.getCharacteristic(CHR_ISSC_TRANS_RX);
                           gatt.setCharacteristicNotification(mTransTx, true);
                           BluetoothGattDescriptor dsc = mTransTx.getDescriptor(DES_CLIENT_CHR_CONFIG);
                           dsc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                           boolean success = gatt.writeDescriptor(dsc);
                           Log.d(TAG, "writing enable notif descriptor:" + success);
                       }
                   } else {
                       Log.i(TAG, "Unable to discover services.");
                   }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic charac, int status) {
                    Log.d(TAG,"read char, uuid=" + charac.getUuid().toString());
                    byte[] value = charac.getValue();
                    Log.d(TAG, "get value, byte length:" + value.length);
                    for (int i = 0; i < value.length; i++) {
                        Log.d(TAG,"[" + i + "]" + Byte.toString(value[i]));
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic chrc) {
                    Log.d(TAG,"on chr changed" );
                    if (chrc.getUuid().equals(CHR_ISSC_TRANS_TX)) {
                        Log.d(TAG, "Received :" + new String(chrc.getValue()));
                    }
                }

                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic charac, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "Write succeded :" +charac.getValue().length);
                    } else {
                        Log.d(TAG, "Write failed :" +charac.getValue().length);
                    }
                }

                @Override
                public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor dsc, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        byte[] value = dsc.getValue();
                        Log.d(TAG, "Write descriptor success :" +value.length);
                        mTransRx.setValue("Hello world!".getBytes());
                        gatt.writeCharacteristic(mTransRx);
                    }
                }
            };


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
                    mLeDeviceListAdapter = new ArrayList<>();
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                    try { Thread.sleep(3*1000); } catch (InterruptedException e) {}
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    for (BluetoothDevice device : mLeDeviceListAdapter) {
                        BluetoothGatt mBluetoothGatt = device.connectGatt(StaticData.globalContext, false, mGattCallback);
                        try { Thread.sleep(15*1000); } catch (InterruptedException e) {}

                        mBluetoothGatt.close();
                    }
                }
            }
		}
	}

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            Log.i(TAG, "Service with uuid: " + uuid);
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {

                uuid = gattCharacteristic.getUuid().toString();
                Log.i(TAG, " - characteristic with uuid: " + uuid);
            }
        }
    }




        @Override
	public DataField[] getOutputStructure() {
		ArrayList<DataField> output = new ArrayList<DataField>();
		for (int i = 0; i < FIELD_NAMES.length; i++)
			output.add(new DataField(FIELD_NAMES[i], FIELD_TYPES_STRING[i],
					                        FIELD_DESCRIPTION[i]));

		return output.toArray(new DataField[]{});
	}

	@Override
	public String[] getFieldList() {
		return FIELD_NAMES;
	}

	@Override
	public Byte[] getFieldType() {
		return FIELD_TYPES;
	}


	public static class SerialBLEService extends WrapperService {

		public SerialBLEService() {
			super("serialBTService");

		}
	}

}