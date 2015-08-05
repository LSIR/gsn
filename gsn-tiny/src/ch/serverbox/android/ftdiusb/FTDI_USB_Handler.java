/*
 * This file is part of Android FTDI Serial
 *
 * Copyright (C) 2011 - Manuel Di Cerbo, Nexus-Computing GmbH
 *
 * Android FTDI Serial is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Android FTDI Serial is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Android FTDI Serial; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 */

/* 
 * Thanks to the libftdi project http://www.intra2net.com/en/developer/libftdi/
 */
package ch.serverbox.android.ftdiusb;

import java.util.HashMap;
import java.util.Iterator;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;


//0403:6001 FTDI Serial
//0x81 EP IN
//0x02 EP OUT
/*
 * 	handle.controlMsg(requestType = 0x40, request = 0, value = 0, index = 0, buffer = 0, timeout = 0)#reset
 handle.controlMsg(requestType = 0x40, request = 0, value = 1, index = 0, buffer = 0, timeout = 0)#reset
 handle.controlMsg(requestType = 0x40, request = 0, value = 2, index = 0, buffer = 0, timeout = 0)#reset
 handle.controlMsg(requestType = 0x40, request = 0x03, value = 0x4138, index = 0, buffer = 0, timeout = 0)#9600 baudrate
 */
public class FTDI_USB_Handler {
	protected static final String ACTION_USB_PERMISSION = "ch.serverbox.android.USB";
	private static final String VID_PID = "0403:6001";
	private UsbDeviceConnection conn = null;
	private UsbInterface usbIf = null;
	private UsbEndpoint epIN = null;
	private UsbEndpoint epOUT = null;
	protected Context activity = null;
	private boolean mStopped = true;
	protected Thread readThread = null;

	public FTDI_USB_Handler(final Context a) {
		activity = a;

		readThread = new Thread() {
			public void run() {
				if (epIN != null) {
					while (!isInterrupted()) {
						int size;
						byte[] buffer = new byte[64];
						if (conn == null)
							return;
						size = conn.bulkTransfer(epIN, buffer, buffer.length, 0);
						if (size > 2) {
							byte[] buf_out = new byte[64];
							for (int i = 2; i < buffer.length; i++) {
								buf_out[i - 2] = buffer[i];
							}
							onDataReceived(buf_out, size - 2);
						}
					}
				}
			}
		};
		activity = a;
		if (mStopped)
			enumerate();
	}

	public void Stop() {
		if (readThread.isAlive())
			readThread.interrupt();
		if (usbIf != null && conn != null) {
			activity.unregisterReceiver(mPermissionReceiver);
			conn.releaseInterface(usbIf);
			conn.close();
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				UsbDevice dev = (UsbDevice) intent
						.getParcelableExtra(UsbManager.EXTRA_DEVICE);
				if (dev != null) {
					if (String.format("%04X:%04X", dev.getVendorId(), dev.getProductId())
							.equals(VID_PID)) {
						if (usbIf != null && conn != null) {
							conn.releaseInterface(usbIf);
							conn.close();
						}
					}
				}
			}
		}
	};

	private final BroadcastReceiver mPermissionReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
				if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
					e("Permission not granted :(");
				}
				else {
					l("Permission granted");
					UsbDevice dev = (UsbDevice) intent
							.getParcelableExtra(UsbManager.EXTRA_DEVICE);
					if (dev != null) {
						if (String.format("%04X:%04X", dev.getVendorId(),
								dev.getProductId()).equals(VID_PID)) {
							init_USB(dev);// has new thread
						}
					}
					else {
						e("device not present!");
					}
				}
			}
		}
	};

	private void enumerate() {
		l("enumerating");

		UsbManager usbman = (UsbManager) activity
				.getSystemService(Context.USB_SERVICE);

		HashMap<String, UsbDevice> devlist = usbman.getDeviceList();
		Iterator<UsbDevice> deviter = devlist.values().iterator();
		PendingIntent pi = PendingIntent.getBroadcast(activity, 0, new Intent(
				ACTION_USB_PERMISSION), 0);

		while (deviter.hasNext()) {
			UsbDevice d = deviter.next();
			l("Found device: "
					+ String.format("%04X:%04X", d.getVendorId(), d.getProductId()));
			if (String.format("%04X:%04X", d.getVendorId(), d.getProductId()).equals(
					VID_PID)) {
				// we need to upload the hex file, first request permission
				l("Device under: " + d.getDeviceName());
				activity.registerReceiver(mPermissionReceiver, new IntentFilter(
						ACTION_USB_PERMISSION));
				if (!usbman.hasPermission(d))
					usbman.requestPermission(d, pi);
				else
					init_USB(d);

				// init_USB(d);
				break;
			}
		}
		l("no more devices found");
	}

	private boolean init_USB(UsbDevice dev) {
		showLog("Init_USB");
		
		try {
			if (dev == null)
				return false;
			UsbManager usbm = (UsbManager) activity
					.getSystemService(Context.USB_SERVICE);
			conn = usbm.openDevice(dev);
			l("Interface Count: " + dev.getInterfaceCount());
			l("Using "
					+ String.format("%04X:%04X", dev.getVendorId(), dev.getProductId()));

			if (!conn.claimInterface(dev.getInterface(0), true))
				return false;

			conn.controlTransfer(0x40, 0, 0, 0, null, 0, 0);// reset
			conn.controlTransfer(0x40, 0, 1, 0, null, 0, 0);// clear Rx
			conn.controlTransfer(0x40, 0, 2, 0, null, 0, 0);// clear Tx
			conn.controlTransfer(0x40, 0x02, 0x1311, 0x40, null, 0, 0);// control flow
																																	// XOFF/XON
			// conn.controlTransfer(0x40, 0x04, 0x0008, 0, null, 0, 0); //data bit 8,
			// parity none, stop bit 1, tx off
			// conn.controlTransfer(0x40, 0x03, 0x809C, 0, null, 0, 0);//baudrate
			// 19200
			conn.controlTransfer(0x40, 0x03, 0x4138, 0, null, 0, 0);// baudrate 9600

			usbIf = dev.getInterface(0);
			for (int i = 0; i < usbIf.getEndpointCount(); i++) {
				l("EP: " + String.format("0x%02X", usbIf.getEndpoint(i).getAddress()));
				if (usbIf.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
					l("Bulk Endpoint");
					if (usbIf.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN)
						epIN = usbIf.getEndpoint(i);
					else
						epOUT = usbIf.getEndpoint(i);
				}
				else {
					l("Not Bulk");
				}
			}

			activity.registerReceiver(mUsbReceiver, new IntentFilter(
					UsbManager.ACTION_USB_DEVICE_DETACHED));
			l("init_USB: epIN " + epIN + ", epOUT: " + epOUT);

			readThread.start();
		}
		catch (final Exception e) {
			 e.printStackTrace();
		}

		showLog("init_USB finish");
		
		return (epIN != null && epOUT != null);
	}

	public void write(final byte[] b) {
		if (epOUT != null) {
			new Thread(new Runnable() {
				public void run() {
					conn.bulkTransfer(epOUT, b, b.length, 0);
					l("writing : " + new String(b));
				}
			}).start();
		}
//		showLog("write finish");
	}

	protected void onDataReceived(byte[] buffer, int size) {
		l(buffer);
	}

	protected void l(Object s) {
		Log.d("FTDI_USB", ">==< " + s.toString() + " >==<");
//		showLog(s.toString());
	}

	protected void e(Object s) {
		Log.e("FTDI_USB", ">==< " + s.toString() + " >==<");
		showLog(s.toString());
	}
	
	void showLog(final String text){
		 Log.i("FTDI_USB", text);
	}
}