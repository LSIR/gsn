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
package com.nordicsemi.nrfUARTv2;

//import java.io.Serializable;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;


import tinygsn.model.utils.WritableBT;
import tinygsn.model.wrappers.AbstractWrapper;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    public static final String TAG = "SerialBLEWrapper";

	public static final String[] FIELD_NAMES = new String[]{ "sensor", "modality", "measure"};

	public static final Byte[] FIELD_TYPES = new Byte[]{DataTypes.VARCHAR, DataTypes.VARCHAR, DataTypes.DOUBLE};

	private static final String[] FIELD_DESCRIPTION = new String[]{"sensor", "modality", "measure"};

	private static final String[] FIELD_TYPES_STRING = new String[]{"varchar(20)", "varchar(32)", "double"};

	public final Class<? extends WrapperService> getSERVICE() {
		return SerialBLEService.class;
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

    @Override
    public void runOnce() { }


    public static class SerialBLEService extends WrapperService implements WritableBT {

		public SerialBLEService() {
			super("serialBTService");
		}

        @Override
        protected void onHandleIntent(Intent intent) {
            Bundle b = intent.getBundleExtra("tinygsn.beans.config");
            config = b.getParcelable("tinygsn.beans.config");
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!config.isRunning()) {
                am.cancel(PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                return;
            }
            long interval = 10;
            try {
                w = StaticData.getWrapperByName(config.getWrapperName());
                w.updateWrapperInfo();
                if (w.getDcDuration() > 0) {
                    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (mBluetoothAdapter != null) {
                        if (!mBluetoothAdapter.isEnabled()) {
                            ToastUtils.showToastInUiThread(StaticData.globalContext, "The Bluetooth wrapper needs Bluetooth to be enabled.", Toast.LENGTH_LONG);
                        } else {
                            service_init();
                            mBluetoothAdapter.cancelDiscovery();
                            mLeDeviceListAdapter = new ArrayList<>();
                            mBluetoothAdapter.startLeScan(mLeScanCallback);
                            try { Thread.sleep(10*1000); } catch (InterruptedException e) {}
                            mBluetoothAdapter.stopLeScan(mLeScanCallback);

                            for (BluetoothDevice device : mLeDeviceListAdapter) {
                                    mService.connect(device.getAddress());
                                    cdl = new CountDownLatch(1);
                                    cdl.await(60, TimeUnit.SECONDS);
                                    mService.disconnect();
                            }
                            try {
                                LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
                            } catch (Exception ignore) {
                                Log.e(TAG, ignore.toString());
                            }
                            unbindService(mServiceConnection);
                            mService.stopSelf();
                            mService = null;
                        }
                    }
                }
                interval = w.getDcInterval();
            } catch (Exception e) {
                Log.e("WrapperService["+this.getClass().getName()+"]", e.getMessage());
            }
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * interval, PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
        }

        private CountDownLatch cdl;
        private AbstractSerialProtocol sensor;

        private ArrayList<BluetoothDevice> mLeDeviceListAdapter;
        private UartService mService = null;

        private BluetoothAdapter.LeScanCallback mLeScanCallback =
                new BluetoothAdapter.LeScanCallback() {
                    @Override
                    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                        if (!mLeDeviceListAdapter.contains(device)) {
                            mLeDeviceListAdapter.add(device);
                            Log.i(TAG, "Found device: " + device.getAddress());
                        }
                    }
                };

        //UART service connected/disconnected
        private ServiceConnection mServiceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder rawBinder) {
                mService = ((UartService.LocalBinder) rawBinder).getService();
                Log.d(TAG, "onServiceConnected mService= " + mService);
                if (!mService.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth");
                }

            }

            public void onServiceDisconnected(ComponentName classname) {
                ////     mService.disconnect(mDevice);
                mService = null;
            }
        };

        private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

            private String buffer = "";

            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                final Intent mIntent = intent;
                //*********************//
                if (action.equals(UartService.ACTION_GATT_CONNECTED)) {

                    Log.d(TAG, "UART_CONNECT_MSG");
                }

                //*********************//
                if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                    Log.d(TAG, "UART_DISCONNECT_MSG");
                    cdl.countDown();
                    //mService.close();
                }


                //*********************//
                if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                    mService.enableTXNotification();
                    sensor = new OpenSWISSensor(SerialBLEService.this, mService.getAddress().replace(":","_"));
                    sensor.getMeasurements();
                }
                //*********************//
                if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                    final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                    try {
                        buffer += new String(txValue, "UTF-8");

                        if (buffer.contains("\n")) {
                            String[] st = buffer.split("\n");
                            for (String s:st){
                                if (s.trim().isEmpty()) continue;
                                sensor.received(s);
                            }
                        }

                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
                //*********************//
                if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
                    //"Device doesn't support UART. Disconnecting");
                    cdl.countDown();
                   // mService.disconnect();

                }


            }
        };


        private void service_init() {
            Intent bindIntent = new Intent(this, UartService.class);
            bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

            LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
        }
        private static IntentFilter makeGattUpdateIntentFilter() {
            final IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
            intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
            intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
            intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
            intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
            return intentFilter;
        }


        @Override
        public void write(byte[] b) {
            mService.writeRXCharacteristic(b);
        }

        @Override
        public void done() {
           cdl.countDown();
        }

        @Override
        public void publish(StreamElement se){
            w.postStreamElement(se);
        }
    }

}