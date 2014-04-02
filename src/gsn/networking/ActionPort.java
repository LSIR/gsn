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
* File: src/gsn/networking/ActionPort.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package gsn.networking;

import gsn.utils.ValidityTools;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;

public class ActionPort {

	public static void listen(final int port,final NetworkAction action){
		Thread t = new Thread(){
			ServerSocket ss;
			Logger logger = Logger.getLogger ( this.getClass() );
			{
				try {
					ss = new ServerSocket(port, 0, InetAddress.getByName("localhost"));
				}
				catch (Exception e) {
					logger.error (e.getMessage(), e) ;
				}
			}
			public void run () {

				boolean running = true;

				while (running ) {
					try {
						Socket socket = ss.accept();
						if (logger.isDebugEnabled())
							logger.debug("Opened connection on control socket.");
						socket.setSoTimeout(30000);

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
						running = action.actionPerformed(socket);

					} catch (SocketTimeoutException e) {
						if (logger.isDebugEnabled())
							logger.debug("Connection timed out. Message was: " + e.getMessage());
					} catch (IOException e) {
						logger.warn("Error while accepting control connection: " + e.getMessage());
					}
				}
			}
		};
		t.start();
	}
}
