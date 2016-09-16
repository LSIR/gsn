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
* File: src/ch/epfl/gsn/utils/SensorScopeListenerTest.java
*
* @author Sofiane Sarni
*
*/

package ch.epfl.gsn.utils;
import java.net.*;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public class SensorScopeListenerTest
{
    private static transient Logger logger = LoggerFactory.getLogger(SensorScopeListenerTest.class);
    public SensorScopeListenerTest(int port)
    {
        ServerSocket server;

        try
        {
            server = new ServerSocket(port);

            while(true)
            {
                try
                {
                    Socket socket = server.accept();

                    if(socket != null)
                        new SensorScopeListenerClient(socket);
                }
                catch(Exception e)
                {
                    System.out.println("Error while accepting a new client: " + e);
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("Could not create the server: " + e);
        }
    }

    public static void main(String args[])
    {
        new SensorScopeListenerTest(1234);
    }
}

