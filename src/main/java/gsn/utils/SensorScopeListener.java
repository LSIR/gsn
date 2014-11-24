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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/gsn/utils/SensorScopeListener.java
*
* @author Sofiane Sarni
*
*/

package gsn.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class SensorScopeListener {
    public static final String CONF_LOG4J_SENSORSCOPE_PROPERTIES = "conf/log4j_sensorscope.properties";
    private static final String CONF_SENSORSCOPE_SERVER_PROPERTIES = "conf/sensorscope_server.properties";
    private static final String DEFAULT_FOLDER_FOR_CSV_FILES = "logs";

    private static transient Logger logger = Logger.getLogger(SensorScopeListener.class);
    private static String csvFolderName = null;
    private static String DEFAULT_NULL_STRING = "null";
    private static String nullString = DEFAULT_NULL_STRING;
    private static int port;

    public SensorScopeListener(int port) {
        ServerSocket server;

        logger.info("Starting server on port " + port);

        try {
            server = new ServerSocket(port);

            while (true) {
                try {
                    Socket socket = server.accept();

                    if (socket != null)
                        new SensorScopeListenerClient(socket);
                } catch (Exception e) {
                    logger.error("Error while accepting a new client: " + e);
                }
            }
        } catch (Exception e) {
            logger.error("Could not create the server: " + e);
        }
    }

    public static void config() {
        Properties propertiesFile = new Properties();
        try {
            propertiesFile.load(new FileInputStream(CONF_SENSORSCOPE_SERVER_PROPERTIES));
        } catch (IOException e) {
            logger.error("Couldn't load configuration file: " + CONF_SENSORSCOPE_SERVER_PROPERTIES);
            logger.error(e.getMessage(), e);
            System.exit(-1);
        }

        csvFolderName = propertiesFile.getProperty("csvFolder", DEFAULT_FOLDER_FOR_CSV_FILES);
        nullString = propertiesFile.getProperty("nullString", DEFAULT_NULL_STRING);

        String str_port = propertiesFile.getProperty("serverPort");

        if (str_port == null) {
            logger.error("Couldn't find serverPort value in configuration file: " + CONF_SENSORSCOPE_SERVER_PROPERTIES);
            System.exit(-1);
        }
        try {
            port = Integer.parseInt(str_port);
        } catch (NumberFormatException e) {
            logger.error("Incorrect value (" + str_port + ") for serverPort in configuration file: " + CONF_SENSORSCOPE_SERVER_PROPERTIES);
            System.exit(-1);
        }
    }

    public static void main(String args[]) {
        config();
        PropertyConfigurator.configure(CONF_LOG4J_SENSORSCOPE_PROPERTIES);
        new SensorScopeListener(port);
    }
}

