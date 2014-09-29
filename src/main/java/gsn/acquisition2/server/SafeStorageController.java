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
* File: src/gsn/acquisition2/server/SafeStorageController.java
*
* @author Timotee Maret
* @author Ali Salehi
*
*/

package gsn.acquisition2.server;

import gsn.networking.ActionPort;
import gsn.networking.NetworkAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.apache.log4j.Logger;

public class SafeStorageController {

	public static final String SAFE_STORAGE_SHUTDOWN = "SS SHUTDOWN";

	public static transient Logger logger = Logger.getLogger(SafeStorageController.class);

	public SafeStorageController(final SafeStorageServer safeStorageServer, int safeStorageControllerPort) {
		super();
		logger.info("Started Safe Storage Controller on port " + safeStorageControllerPort);
		ActionPort.listen(safeStorageControllerPort, new NetworkAction(){
			public boolean actionPerformed(Socket socket) {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					String incomingMsg = reader.readLine();
					
					if (incomingMsg != null && incomingMsg.equalsIgnoreCase(SAFE_STORAGE_SHUTDOWN)) {
						safeStorageServer.shutdown();
						return false;
					}
					else return true;
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
					return false;
				}
			}});
	}
}
