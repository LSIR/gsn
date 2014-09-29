/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2014, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/GSNController.java
*
* @author Jerome Rousselot
* @author Mehdi Riahi
* @author gsn_devs
* @author Timotee Maret
* @author Ali Salehi
*
*/

package gsn;

import gsn.utils.ValidityTools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;

public class GSNController extends Thread {

	private ServerSocket mySocket;

	private static int gsnControllerPort;

	private static final int GSN_CONTROL_READ_TIMEOUT = 20000;

	public static final String GSN_CONTROL_SHUTDOWN = "GSN STOP";

	public static final String GSN_CONTROL_LIST_LOADED_VSENSORS = "LIST LOADED VSENSORS";

	public static transient Logger logger = Logger.getLogger(GSNController.class);

	private VSensorLoader vsLoader;

	public GSNController(VSensorLoader vsLoader, int gsnControllerPort) throws UnknownHostException, IOException {
		this.vsLoader = vsLoader;
		this.gsnControllerPort = gsnControllerPort ;
		mySocket = new ServerSocket(gsnControllerPort, 0, InetAddress.getByName("localhost"));
		this.start();
	}

	public void run() {
		logger.info("Started GSN Controller on port " + gsnControllerPort);
		while (true) {
			try {
				Socket socket = mySocket.accept();
				if (logger.isDebugEnabled())
					logger.debug("Opened connection on control socket.");
				socket.setSoTimeout(GSN_CONTROL_READ_TIMEOUT);

				// Only connections from localhost are allowed
				if (ValidityTools.isLocalhost(socket.getInetAddress().getHostAddress()) == false) {
					try {
						logger.warn("Connection request from IP address >" + socket.getInetAddress().getHostAddress() + "< was denied.");
						socket.close();
					} catch (IOException ioe) {
						// do nothing
					}
					continue;
				}
				new StopManager().start();
			} catch (SocketTimeoutException e) {
				if (logger.isDebugEnabled())
					logger.debug("Connection timed out. Message was: " + e.getMessage());
			} catch (IOException e) {
				logger.warn("Error while accepting control connection: " + e.getMessage());
			}
		}
	}

	/*
	 * This method must be called after virtual sensors initialization. It
	 * allows GSNController to shut down properly all the virtual sensors in
	 * use.
	 */
	public void setLoader(VSensorLoader vsLoader) {
		if (this.vsLoader == null) // override protection
			this.vsLoader = vsLoader;
	}

	private class StopManager extends Thread {

		public void run() {

			new Thread(new Runnable() {

				public void run() {
					try {
						Thread.sleep(7000);
					} catch (InterruptedException e) {

					}finally {
						logger.warn("Forced exit...");
						System.exit(1);
					}
				}}).start();

			try {
				// We  stop  GSN  here
				logger.info("Shutting down GSN...");
				if (vsLoader != null) {
					vsLoader.stopLoading();
					logger.info("All virtual sensors have been stopped, shutting down virtual machine.");
				} else {
					logger.warn("Could not shut down virtual sensors properly. We are probably exiting GSN before it has been completely initialized.");
				}
			} catch (Exception e) {
				logger.warn("Error while reading from or writing to control connection: " + e.getMessage(), e);
			}finally {
			}
		}
	}
}
