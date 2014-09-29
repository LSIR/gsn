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
* File: src/org/tempuri/ExtensionMapper.java
*
* @author Ali Salehi
* @author Timotee Maret
*
*/


/**
 * ExtensionMapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:41 LKT)
 */

            package org.tempuri;
            /**
            *  ExtensionMapper class
            */
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "ArrayOfDouble".equals(typeName)){
                   
                            return  org.tempuri.ArrayOfDouble.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "BasicSensorData".equals(typeName)){
                   
                            return  org.tempuri.BasicSensorData.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "ArrayOfSensorData".equals(typeName)){
                   
                            return  org.tempuri.ArrayOfSensorData.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "BinarySensorData".equals(typeName)){
                   
                            return  org.tempuri.BinarySensorData.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "ArrayOfBase64Binary".equals(typeName)){
                   
                            return  org.tempuri.ArrayOfBase64Binary.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "SensorData".equals(typeName)){
                   
                            return  org.tempuri.SensorData.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://microsoft.com/wsdl/types/".equals(namespaceURI) &&
                  "guid".equals(typeName)){
                   
                            return  com.microsoft.wsdl.types.Guid.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "SensorInfo".equals(typeName)){
                   
                            return  org.tempuri.SensorInfo.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "ArrayOfSensorInfo".equals(typeName)){
                   
                            return  org.tempuri.ArrayOfSensorInfo.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "ArrayOfString".equals(typeName)){
                   
                            return  org.tempuri.ArrayOfString.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "ArrayOfDateTime".equals(typeName)){
                   
                            return  org.tempuri.ArrayOfDateTime.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://tempuri.org/".equals(namespaceURI) &&
                  "ArrayOfGuid".equals(typeName)){
                   
                            return  org.tempuri.ArrayOfGuid.Factory.parse(reader);
                        

                  }

              
             throw new org.apache.axis2.databinding.ADBException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    