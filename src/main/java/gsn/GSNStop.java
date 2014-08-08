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
* File: src/gsn/GSNStop.java
*
* @author Ali Salehi
* @author gsn_devs
* @author Timotee Maret
*
*/

package gsn;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class GSNStop {
  
  public static void main(String[] args) {
    stopGSN(Integer.parseInt(args[0]));
  }
  public static void stopGSN(int gsnControllerPort){
    try {
//      Socket socket = new Socket(InetAddress.getLocalHost().getLocalHost(), gsn.GSNController.GSN_CONTROL_PORT);
      Socket socket = new Socket(InetAddress.getByName("localhost"), gsnControllerPort);
      PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
      writer.println(gsn.GSNController.GSN_CONTROL_SHUTDOWN);
      writer.flush();
      System.out.println("[Done]");
    }catch (Exception e) {
      System.out.println("[Failed: "+e.getMessage()+ "]");
    }
  }
}
