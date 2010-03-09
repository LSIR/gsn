
/**
 * ExtensionMapper.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:41 LKT)
 */

            package gsn.hydrosys.sensormanager.xsd;
            /**
            *  ExtensionMapper class
            */
        
        public  class ExtensionMapper{

          public static java.lang.Object getTypeObject(java.lang.String namespaceURI,
                                                       java.lang.String typeName,
                                                       javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{

              
                  if (
                  "http://sensormanager.hydrosys.gsn/xsd".equals(namespaceURI) &&
                  "DataField".equals(typeName)){
                   
                            return  gsn.hydrosys.sensormanager.xsd.DataField.Factory.parse(reader);
                        

                  }

              
                  if (
                  "http://sensormanager.hydrosys.gsn/xsd".equals(namespaceURI) &&
                  "Status".equals(typeName)){
                   
                            return  gsn.hydrosys.sensormanager.xsd.Status.Factory.parse(reader);
                        

                  }

              
             throw new org.apache.axis2.databinding.ADBException("Unsupported type " + namespaceURI + " " + typeName);
          }

        }
    