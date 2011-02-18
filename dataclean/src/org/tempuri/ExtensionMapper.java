
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
    