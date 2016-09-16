/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/monitoring/MonitoringServer.java
*
* @author Julien Eberle
*
*/

package ch.epfl.gsn.monitoring;

import javax.net.ServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.epfl.gsn.Main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Map;



public class MonitoringServer extends Thread {
	
	private final transient Logger logger = LoggerFactory.getLogger( MonitoringServer.class );
	
	private ServerSocket socket = null;
	private boolean running = true;
	private SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z"); 


	/**
     * Return statistics about the GSN server, faking an HTTP server for retro-compatibility
     * 
     * the protocol is similar to the carbon protocol used by Graphite, except the timestamp
     * http://matt.aimonetti.net/posts/2013/06/26/practical-guide-to-graphite-monitoring/
     */
	public MonitoringServer(int port) {
		super("Monitoring Server");

		try {
			ServerSocketFactory serverSocketFactory = ServerSocketFactory.getDefault();
            socket = serverSocketFactory.createServerSocket(port);
            socket.setSoTimeout(1000);
		}catch(Exception e){
			logger.error("unable to open socket for monitoring",e);
		}
	}
	
	@Override
	public void run(){
	
		while (running){
		   try {
		       final Socket server = socket.accept();
		       server.setSoTimeout(60000);
		       logger.debug("monitoring accepted from "+server.getInetAddress());
               Thread t = new Thread(new Runnable(){

					@Override
					public void run() {
						try {
						InputStream input = new BufferedInputStream(server.getInputStream());
						OutputStream output = new BufferedOutputStream(server.getOutputStream());
						byte[] b = new byte[3];
						input.read(b);
						if (new String(b).equalsIgnoreCase("get")){
							StringBuilder values = new StringBuilder();
					        for (Monitorable m : Main.getInstance().getToMonitor()){
					        	Hashtable<String,Object> h = m.getStatistics();
					        	for (Map.Entry<String,Object> e : h.entrySet()){
					        		values.append(e.getKey()).append(" ");
					        		values.append(e.getValue()).append("\n");
					        	}
					        }
					        byte[] content = values.toString().getBytes("utf-8");

					        String head = "HTTP/1.1 200 OK\r\nDate: " + 
					                sdf.format(new Date()) +
					        		"\r\nServer: Global Sensor Network(GSN)\r\nContent-Length: " + 
					        		content.length +
					        		"\r\nCache-Control: public, max-age=0, no-cache\r\nConnection: close\r\n"+
					        		"Content-Type: text/plain; charset=UTF-8\r\n\r\n";
					        output.write(head.getBytes("utf-8"));
					        output.write(content);
					        output.flush();
						}
						server.close();
						}catch (Exception e){
							logger.warn("Error while communicating with "+server.getInetAddress(), e);
						}
					}
					
			   });
	           t.setName("Monitoring thread");
	           t.start();
	   
	          } catch(SocketTimeoutException ste){
	        	  // just keep trying until someone connects
	          } catch(IOException ioe){
	        	  logger.error("IO Error on monitoring connection.", ioe);
	          }
	    
		}
	}
	
	public void stopServer() {
		try {
			running = false;
			Thread.sleep(1000);
			socket.close();
			Thread.sleep(100);

		}catch(Exception e){}
	}

}
