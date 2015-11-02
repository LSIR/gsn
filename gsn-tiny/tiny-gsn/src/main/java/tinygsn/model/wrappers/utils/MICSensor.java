/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
*
* This file is part of GSN.
*
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with GSN. If not, see <http://www.gnu.org/licenses/>.
*
* File: gsn-tiny/src/tinygsn/model/wrappers/utils/MICSensor.java
*
* @author Do Ngoc Hoan
*/


package tinygsn.model.wrappers.utils;

import java.io.Serializable;
import java.util.Arrays;

import tinygsn.beans.DataField;
import tinygsn.beans.StaticData;
import tinygsn.beans.StreamElement;
import android.content.Context;
import android.os.Handler;
import android.widget.Toast;
import ch.serverbox.android.ftdiusb.FTDI_USB_Handler;

/* 
 * All the communication with the sensor happens here.
 */
public class MICSensor extends FTDI_USB_Handler {
	
	private static MICSensor instance = null;
	
	public static MICSensor getInstance(){
		if (instance == null){
			instance = new MICSensor(StaticData.globalContext);
		}
		return instance;
	}
	

	public interface ReadyListener {
		public void onReady();
	}

	public interface VirtualSensorDataListener {
		public void consume(StreamElement se);
	}
	private VirtualSensorDataListener listener = null;
	private ReadyListener initListener = null;

	private DataField[] df;

	// Calibration values
	private double k = 0.025;
	private double a0 = 0.7;
	private double a1 = 0.02;
	private double b0 = 0.7;
	private double b1 = 0.02;
	private int interval = 30;

	// Create an handler for drawing toast messages from other threads etc.
	private Handler handler;

	// Boolean flags to check for sensor answers etc
	private boolean waitForDiagnostic;
	// private boolean waitForAuto;
	private boolean waitForStop;
	// private boolean waitForTimer;
	private boolean measurementReceived;
	private boolean sensorInitialized;
	protected boolean saveToFile;
	private boolean autoMeasurementThreadFlag;
	private boolean initThreadFlag;
	private boolean stopThreadFlag;

	private char[] receiveBuffer; // A buffer to store the received bytes
	private int bufferIndex; // The buffer index for the receive buffer

	private MICSensor(Context a) {
		super(a);

		try {
			df = new DataField[6];
			df[0] = new DataField("resistanceO", "int");
			df[1] = new DataField("resistanceV", "int");
			df[2] = new DataField("humidity", "double");
			df[3] = new DataField("temperature", "double");
			df[4] = new DataField("ozoneCalibrated", "double");
			df[5] = new DataField("vocCalibrated", "double");
		}
		catch (Exception e) {

		}

		handler = new Handler();

		// Initialize the receive buffer
		bufferIndex = 0;
		receiveBuffer = new char[32]; // 32 is the largest string size for the
																	// sensor.
		waitForDiagnostic = false;
		measurementReceived = false;
		sensorInitialized = false;
		autoMeasurementThreadFlag = false;
		initThreadFlag = false;
		stopThreadFlag = false;

	}

	public void setListener(VirtualSensorDataListener listener) {
		this.listener = listener;
	}

	public void setInitListener(ReadyListener initListener) {
		this.initListener = initListener;
	}

	public DataField[] getDataField() {
		return df;
	}

	/*
	 * Toggle automatic measurements. The measurements are polled in the interval
	 * specified in the preferences. Handled in its own thread so it is
	 * non-blocking.
	 */
	public boolean toggleAutoMeasurements(boolean isChecked) {
		Thread thread = new Thread() {
			public void run() {
				while (autoMeasurementThreadFlag) {
					getMeasurement();
					try {
						// Sleep for interval seconds.
						sleep(interval * 1000);
					}
					catch (InterruptedException e) {
						e.printStackTrace();
					}
					// If no answer was received, wait a bit longer.
					// If we try to get a reading when the sensor is updating itself, it
					// doesn't respond.
					if (measurementReceived == false) {
						try {
							sleep(200);
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
		// Start the auto update thread when the button is on.
		if (isChecked) {
			// If the sensor couldn't initialize, we don't start the thread.
			/*
			 * if(sensorInitialized==false) { Toast.makeText(activity,
			 * "Sensor not initialized", Toast.LENGTH_SHORT).show(); // Return false
			 * to indicate that the button has to be turned off again.
			 * 
			 * return false; } else {
			 */
			autoMeasurementThreadFlag = true;
			thread.start();
			// }
		}
		else {
			autoMeasurementThreadFlag = false;
		}
		return true;
	}

	/*
	 * Send a {M} to get a measurement from the sensor. The received data is
	 * handled in the onDataReceived routine.
	 */
	public void getMeasurement() {
		if (sensorInitialized == false) {
//			Toast
//					.makeText(activity, "Sensor not yet initialized", Toast.LENGTH_SHORT)
//					.show();
			initSensor();
			return;
		}
		measurementReceived = false;
		write("{M}\n".getBytes());
	}

	/*
	 * Process a read input string.
	 */
	private void processInput(String receivedString, int length) {
		// Valid strings start with { and end with }. Anything else is discarded.
		if (receivedString.charAt(0) != '{'
				|| receivedString.charAt(length - 1) != '}') {
			return;
		}

		// A diagnostic message is received.
		// {A00}->OK
		if (receivedString.charAt(1) == 'A' && receivedString.charAt(2) == '0'
				&& receivedString.charAt(3) == '0') {
			waitForDiagnostic = false;
		}
		else if (receivedString.charAt(1) == 'Q') {
			waitForStop = false;
		}
		// If it is a measurement, extract the values.
		// A correct measurement always has length 18.
		else if (receivedString.charAt(1) == 'M') {
			measurementReceived = true;
			// Convert the received string to hex values.
			String[] values = receivedString.split("[,|}|{M]");
			// split string:
			// pos 1: temp
			// pos 2: humidity
			// pos 3: resistance VOC
			// pos 4: resistance O3
			 int resistanceV = Integer.parseInt(values[4]);
			 int resistanceO = Integer.parseInt(values[5]);

			double temperature = Double.parseDouble(values[2]);
			double humidity = Double.parseDouble(values[3]);
			double resistanceCompensatedO = resistanceO
					* Math.exp(k * ((temperature - 25)));
			double resistanceCompensatedV = resistanceV
					* Math.exp(k * ((temperature - 25)));
			double ozoneCalculated = a0 + a1 * resistanceCompensatedO;
			double vocCalculated = b0 + b1 * resistanceCompensatedV;

			StreamElement se = new StreamElement(df, new Serializable[] {
					resistanceO, resistanceV, humidity, temperature, ozoneCalculated,
					vocCalculated });
			if (listener != null) {
				listener.consume(se);
			}
		}
	}

	/*
	 * Initialize the sensor. Do it in its own thread, to avoid blocking. {A},
	 * {S}, {W...} is sent sequentially. After every step we wait for the correct
	 * answer. If no answer is received for 1 second, the init thread is aborted.
	 */
	public void initSensor() {
		// The thread which sends the commands and waits for the answers from the
		// sensor.
		Runnable runnable = new Runnable() {
			public void run() {

				if (initThreadFlag)
					return;

				int counter = 0;

				// Set the thread flag.
				initThreadFlag = true;

				// Wait for a diagnostics message.
				waitForDiagnostic = true;
				// Send a {A} to get a diagnostics answer from the sensor.
				// The sensor should send a {A00} back.
				write("{A}\n".getBytes());
				/*handler.post(new Runnable() {

					public void run() {
						Toast.makeText(activity.getApplicationContext(),
								"Initialization started", Toast.LENGTH_SHORT).show();
					}
				});*/
				l("diagnostic sent");
				while (waitForDiagnostic == true) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
					}
					counter++;
					if (counter > 600) {
						initThreadFlag = false;
						initToast(false);
						return;
					}
				}

				// Successfully initialized.
				sensorInitialized = true;
				// Clear the thread flag.
				initThreadFlag = false;
				initToast(true);
				
//				showLog("Successfully initialized.");
			}

			private void initToast(boolean success) {

				if (success) {
					handler.post(new Runnable() {

						public void run() {
							if (initListener != null)
								initListener.onReady();
							//Toast.makeText(activity.getApplicationContext(),"Initialization succeded", Toast.LENGTH_SHORT).show();
						}
					});
				}
				/*else {
					handler.post(new Runnable() {

						public void run() {
							Toast.makeText(activity.getApplicationContext(),
									"Initialization failed", Toast.LENGTH_SHORT).show();
						}
					});
				}*/
			}
		};

		// Start the sensor init thread.
		if (initThreadFlag == false) {
			new Thread(runnable).start();
		}
		else {
//			Toast.makeText(activity, "Already initializing", Toast.LENGTH_SHORT)
//					.show();
		}
	}

	/*
	 * Pause the sensor. The sensor has to be reinitialized after that
	 */
	public void Pause() {
		// The thread which sends the commands and waits for the answers from the
		// sensor.
		Runnable runnable = new Runnable() {
			public void run() {

				int counter = 0;

				// Set the thread flag.
				stopThreadFlag = true;

				// Wait for a diagnostics message.
				waitForStop = true;
				// Send a {A} to get a diagnostics answer from the sensor.
				// The sensor should send a {A00} back.
				write("{Q}\n".getBytes());
				handler.post(new Runnable() {

					public void run() {
						Toast.makeText(activity.getApplicationContext(), "Sensor stopped",
								Toast.LENGTH_SHORT).show();
					}
				});
				l("stop sent");
				while (waitForStop == true) {
					try {
						Thread.sleep(100);
					}
					catch (InterruptedException e) {
					}
					counter++;
					if (counter > 50) {
						stopThreadFlag = false;
						return;
					}
				}

				// Successfully uninitialized.
				sensorInitialized = false;
				// Clear the thread flag.
				stopThreadFlag = false;
			}
		};

		// Start the sensor init thread.
		if (stopThreadFlag == false) {
			new Thread(runnable).start();
		}
		else {
			Toast.makeText(activity, "Already uninitializing", Toast.LENGTH_SHORT)
					.show();
		}
	}

	/*
	 * Process any data that's coming from the usb port.
	 */
	@Override
	protected void onDataReceived(final byte[] buffer, final int size) {

		new Thread(new Runnable() {
			public void run() {
				l("receive:" + new String(buffer, 0, size));
//				 showLog("onDataReceived " + "receive:" + new String(buffer, 0,
//				 size));

				if (receiveBuffer != null) {
					for (int i = 0; i < size; i++) {
						if ((char) buffer[i] == '{') {
							bufferIndex = 0;
							Arrays.fill(receiveBuffer, (char) 0);
						}
						// Write the read byte into the receive buffer.
						receiveBuffer[bufferIndex] = (char) buffer[i];
						// If a } is received, process the read data.
						if (receiveBuffer[bufferIndex] == '}') {
							processInput(new String(receiveBuffer), bufferIndex + 1);
							// Clear the buffer.
							bufferIndex = 0;
							Arrays.fill(receiveBuffer, (char) 0);
						}
						else {
							// Update the buffer index. Don't let it overflow.
							bufferIndex = bufferIndex >= receiveBuffer.length - 1 ? receiveBuffer.length - 1
									: bufferIndex + 1;
						}
					}
				}
			}
		}).start();
	}

//	void showLog(final String text) {
//		activity.runOnUiThread(new Runnable() {
//			public void run() {
//				Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
//			}
//		});
//	}
}

