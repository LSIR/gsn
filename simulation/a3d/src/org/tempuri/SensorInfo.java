
/**
 * SensorInfo.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:41 LKT)
 */
            
                package org.tempuri;
            

            /**
            *  SensorInfo bean class
            */
        
        public  class SensorInfo
        implements org.apache.axis2.databinding.ADBBean{
        /* This type was generated from the piece of schema that had
                name = SensorInfo
                Namespace URI = http://tempuri.org/
                Namespace Prefix = ns1
                */
            

        private static java.lang.String generatePrefix(java.lang.String namespace) {
            if(namespace.equals("http://tempuri.org/")){
                return "ns1";
            }
            return org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
        }

        

                        /**
                        * field for PublisherName
                        */

                        
                                    protected java.lang.String localPublisherName ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localPublisherNameTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getPublisherName(){
                               return localPublisherName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param PublisherName
                               */
                               public void setPublisherName(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localPublisherNameTracker = true;
                                       } else {
                                          localPublisherNameTracker = false;
                                              
                                       }
                                   
                                            this.localPublisherName=param;
                                    

                               }
                            

                        /**
                        * field for SensorName
                        */

                        
                                    protected java.lang.String localSensorName ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localSensorNameTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getSensorName(){
                               return localSensorName;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SensorName
                               */
                               public void setSensorName(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localSensorNameTracker = true;
                                       } else {
                                          localSensorNameTracker = false;
                                              
                                       }
                                   
                                            this.localSensorName=param;
                                    

                               }
                            

                        /**
                        * field for Longitude
                        */

                        
                                    protected double localLongitude ;
                                

                           /**
                           * Auto generated getter method
                           * @return double
                           */
                           public  double getLongitude(){
                               return localLongitude;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Longitude
                               */
                               public void setLongitude(double param){
                            
                                            this.localLongitude=param;
                                    

                               }
                            

                        /**
                        * field for Latitude
                        */

                        
                                    protected double localLatitude ;
                                

                           /**
                           * Auto generated getter method
                           * @return double
                           */
                           public  double getLatitude(){
                               return localLatitude;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Latitude
                               */
                               public void setLatitude(double param){
                            
                                            this.localLatitude=param;
                                    

                               }
                            

                        /**
                        * field for Altitude
                        */

                        
                                    protected double localAltitude ;
                                

                           /**
                           * Auto generated getter method
                           * @return double
                           */
                           public  double getAltitude(){
                               return localAltitude;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Altitude
                               */
                               public void setAltitude(double param){
                            
                                            this.localAltitude=param;
                                    

                               }
                            

                        /**
                        * field for Unit
                        */

                        
                                    protected java.lang.String localUnit ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localUnitTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getUnit(){
                               return localUnit;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Unit
                               */
                               public void setUnit(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localUnitTracker = true;
                                       } else {
                                          localUnitTracker = false;
                                              
                                       }
                                   
                                            this.localUnit=param;
                                    

                               }
                            

                        /**
                        * field for SensorType
                        */

                        
                                    protected java.lang.String localSensorType ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localSensorTypeTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getSensorType(){
                               return localSensorType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SensorType
                               */
                               public void setSensorType(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localSensorTypeTracker = true;
                                       } else {
                                          localSensorTypeTracker = false;
                                              
                                       }
                                   
                                            this.localSensorType=param;
                                    

                               }
                            

                        /**
                        * field for Url
                        */

                        
                                    protected java.lang.String localUrl ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localUrlTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getUrl(){
                               return localUrl;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Url
                               */
                               public void setUrl(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localUrlTracker = true;
                                       } else {
                                          localUrlTracker = false;
                                              
                                       }
                                   
                                            this.localUrl=param;
                                    

                               }
                            

                        /**
                        * field for Keywords
                        */

                        
                                    protected java.lang.String localKeywords ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localKeywordsTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getKeywords(){
                               return localKeywords;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Keywords
                               */
                               public void setKeywords(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localKeywordsTracker = true;
                                       } else {
                                          localKeywordsTracker = false;
                                              
                                       }
                                   
                                            this.localKeywords=param;
                                    

                               }
                            

                        /**
                        * field for Description
                        */

                        
                                    protected java.lang.String localDescription ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDescriptionTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getDescription(){
                               return localDescription;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param Description
                               */
                               public void setDescription(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localDescriptionTracker = true;
                                       } else {
                                          localDescriptionTracker = false;
                                              
                                       }
                                   
                                            this.localDescription=param;
                                    

                               }
                            

                        /**
                        * field for DataType
                        */

                        
                                    protected java.lang.String localDataType ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localDataTypeTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getDataType(){
                               return localDataType;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param DataType
                               */
                               public void setDataType(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localDataTypeTracker = true;
                                       } else {
                                          localDataTypeTracker = false;
                                              
                                       }
                                   
                                            this.localDataType=param;
                                    

                               }
                            

                        /**
                        * field for SamplingPeriod
                        */

                        
                                    protected double localSamplingPeriod ;
                                

                           /**
                           * Auto generated getter method
                           * @return double
                           */
                           public  double getSamplingPeriod(){
                               return localSamplingPeriod;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param SamplingPeriod
                               */
                               public void setSamplingPeriod(double param){
                            
                                            this.localSamplingPeriod=param;
                                    

                               }
                            

                        /**
                        * field for ReportPeriod
                        */

                        
                                    protected double localReportPeriod ;
                                

                           /**
                           * Auto generated getter method
                           * @return double
                           */
                           public  double getReportPeriod(){
                               return localReportPeriod;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param ReportPeriod
                               */
                               public void setReportPeriod(double param){
                            
                                            this.localReportPeriod=param;
                                    

                               }
                            

                        /**
                        * field for EntryTime
                        */

                        
                                    protected java.util.Calendar localEntryTime ;
                                

                           /**
                           * Auto generated getter method
                           * @return java.util.Calendar
                           */
                           public  java.util.Calendar getEntryTime(){
                               return localEntryTime;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param EntryTime
                               */
                               public void setEntryTime(java.util.Calendar param){
                            
                                            this.localEntryTime=param;
                                    

                               }
                            

                        /**
                        * field for WebServiceUrl
                        */

                        
                                    protected java.lang.String localWebServiceUrl ;
                                
                           /*  This tracker boolean wil be used to detect whether the user called the set method
                          *   for this attribute. It will be used to determine whether to include this field
                           *   in the serialized XML
                           */
                           protected boolean localWebServiceUrlTracker = false ;
                           

                           /**
                           * Auto generated getter method
                           * @return java.lang.String
                           */
                           public  java.lang.String getWebServiceUrl(){
                               return localWebServiceUrl;
                           }

                           
                        
                            /**
                               * Auto generated setter method
                               * @param param WebServiceUrl
                               */
                               public void setWebServiceUrl(java.lang.String param){
                            
                                       if (param != null){
                                          //update the setting tracker
                                          localWebServiceUrlTracker = true;
                                       } else {
                                          localWebServiceUrlTracker = false;
                                              
                                       }
                                   
                                            this.localWebServiceUrl=param;
                                    

                               }
                            

     /**
     * isReaderMTOMAware
     * @return true if the reader supports MTOM
     */
   public static boolean isReaderMTOMAware(javax.xml.stream.XMLStreamReader reader) {
        boolean isReaderMTOMAware = false;
        
        try{
          isReaderMTOMAware = java.lang.Boolean.TRUE.equals(reader.getProperty(org.apache.axiom.om.OMConstants.IS_DATA_HANDLERS_AWARE));
        }catch(java.lang.IllegalArgumentException e){
          isReaderMTOMAware = false;
        }
        return isReaderMTOMAware;
   }
     
     
        /**
        *
        * @param parentQName
        * @param factory
        * @return org.apache.axiom.om.OMElement
        */
       public org.apache.axiom.om.OMElement getOMElement (
               final javax.xml.namespace.QName parentQName,
               final org.apache.axiom.om.OMFactory factory) throws org.apache.axis2.databinding.ADBException{


        
               org.apache.axiom.om.OMDataSource dataSource =
                       new org.apache.axis2.databinding.ADBDataSource(this,parentQName){

                 public void serialize(org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
                       SensorInfo.this.serialize(parentQName,factory,xmlWriter);
                 }
               };
               return new org.apache.axiom.om.impl.llom.OMSourcedElementImpl(
               parentQName,factory,dataSource);
            
       }

         public void serialize(final javax.xml.namespace.QName parentQName,
                                       final org.apache.axiom.om.OMFactory factory,
                                       org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter)
                                throws javax.xml.stream.XMLStreamException, org.apache.axis2.databinding.ADBException{
                           serialize(parentQName,factory,xmlWriter,false);
         }

         public void serialize(final javax.xml.namespace.QName parentQName,
                               final org.apache.axiom.om.OMFactory factory,
                               org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter xmlWriter,
                               boolean serializeType)
            throws javax.xml.stream.XMLStreamException, org.apache.axis2.databinding.ADBException{
            
                


                java.lang.String prefix = null;
                java.lang.String namespace = null;
                

                    prefix = parentQName.getPrefix();
                    namespace = parentQName.getNamespaceURI();

                    if ((namespace != null) && (namespace.trim().length() > 0)) {
                        java.lang.String writerPrefix = xmlWriter.getPrefix(namespace);
                        if (writerPrefix != null) {
                            xmlWriter.writeStartElement(namespace, parentQName.getLocalPart());
                        } else {
                            if (prefix == null) {
                                prefix = generatePrefix(namespace);
                            }

                            xmlWriter.writeStartElement(prefix, parentQName.getLocalPart(), namespace);
                            xmlWriter.writeNamespace(prefix, namespace);
                            xmlWriter.setPrefix(prefix, namespace);
                        }
                    } else {
                        xmlWriter.writeStartElement(parentQName.getLocalPart());
                    }
                
                  if (serializeType){
               

                   java.lang.String namespacePrefix = registerPrefix(xmlWriter,"http://tempuri.org/");
                   if ((namespacePrefix != null) && (namespacePrefix.trim().length() > 0)){
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           namespacePrefix+":SensorInfo",
                           xmlWriter);
                   } else {
                       writeAttribute("xsi","http://www.w3.org/2001/XMLSchema-instance","type",
                           "SensorInfo",
                           xmlWriter);
                   }

               
                   }
                if (localPublisherNameTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"publisherName", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"publisherName");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("publisherName");
                                    }
                                

                                          if (localPublisherName==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("publisherName cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localPublisherName);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localSensorNameTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"sensorName", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"sensorName");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("sensorName");
                                    }
                                

                                          if (localSensorName==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("sensorName cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localSensorName);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"longitude", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"longitude");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("longitude");
                                    }
                                
                                               if (java.lang.Double.isNaN(localLongitude)) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("longitude cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localLongitude));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"latitude", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"latitude");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("latitude");
                                    }
                                
                                               if (java.lang.Double.isNaN(localLatitude)) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("latitude cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localLatitude));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"altitude", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"altitude");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("altitude");
                                    }
                                
                                               if (java.lang.Double.isNaN(localAltitude)) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("altitude cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAltitude));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                              if (localUnitTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"unit", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"unit");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("unit");
                                    }
                                

                                          if (localUnit==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("unit cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localUnit);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localSensorTypeTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"sensorType", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"sensorType");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("sensorType");
                                    }
                                

                                          if (localSensorType==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("sensorType cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localSensorType);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localUrlTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"url", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"url");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("url");
                                    }
                                

                                          if (localUrl==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("url cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localUrl);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localKeywordsTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"keywords", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"keywords");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("keywords");
                                    }
                                

                                          if (localKeywords==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("keywords cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localKeywords);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localDescriptionTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"description", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"description");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("description");
                                    }
                                

                                          if (localDescription==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localDescription);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             } if (localDataTypeTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"dataType", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"dataType");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("dataType");
                                    }
                                

                                          if (localDataType==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("dataType cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localDataType);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"samplingPeriod", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"samplingPeriod");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("samplingPeriod");
                                    }
                                
                                               if (java.lang.Double.isNaN(localSamplingPeriod)) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("samplingPeriod cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSamplingPeriod));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"reportPeriod", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"reportPeriod");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("reportPeriod");
                                    }
                                
                                               if (java.lang.Double.isNaN(localReportPeriod)) {
                                           
                                                         throw new org.apache.axis2.databinding.ADBException("reportPeriod cannot be null!!");
                                                      
                                               } else {
                                                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localReportPeriod));
                                               }
                                    
                                   xmlWriter.writeEndElement();
                             
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"entryTime", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"entryTime");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("entryTime");
                                    }
                                

                                          if (localEntryTime==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("entryTime cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEntryTime));
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                              if (localWebServiceUrlTracker){
                                    namespace = "http://tempuri.org/";
                                    if (! namespace.equals("")) {
                                        prefix = xmlWriter.getPrefix(namespace);

                                        if (prefix == null) {
                                            prefix = generatePrefix(namespace);

                                            xmlWriter.writeStartElement(prefix,"webServiceUrl", namespace);
                                            xmlWriter.writeNamespace(prefix, namespace);
                                            xmlWriter.setPrefix(prefix, namespace);

                                        } else {
                                            xmlWriter.writeStartElement(namespace,"webServiceUrl");
                                        }

                                    } else {
                                        xmlWriter.writeStartElement("webServiceUrl");
                                    }
                                

                                          if (localWebServiceUrl==null){
                                              // write the nil attribute
                                              
                                                     throw new org.apache.axis2.databinding.ADBException("webServiceUrl cannot be null!!");
                                                  
                                          }else{

                                        
                                                   xmlWriter.writeCharacters(localWebServiceUrl);
                                            
                                          }
                                    
                                   xmlWriter.writeEndElement();
                             }
                    xmlWriter.writeEndElement();
               

        }

         /**
          * Util method to write an attribute with the ns prefix
          */
          private void writeAttribute(java.lang.String prefix,java.lang.String namespace,java.lang.String attName,
                                      java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
              if (xmlWriter.getPrefix(namespace) == null) {
                       xmlWriter.writeNamespace(prefix, namespace);
                       xmlWriter.setPrefix(prefix, namespace);

              }

              xmlWriter.writeAttribute(namespace,attName,attValue);

         }

        /**
          * Util method to write an attribute without the ns prefix
          */
          private void writeAttribute(java.lang.String namespace,java.lang.String attName,
                                      java.lang.String attValue,javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException{
                if (namespace.equals(""))
              {
                  xmlWriter.writeAttribute(attName,attValue);
              }
              else
              {
                  registerPrefix(xmlWriter, namespace);
                  xmlWriter.writeAttribute(namespace,attName,attValue);
              }
          }


           /**
             * Util method to write an attribute without the ns prefix
             */
            private void writeQNameAttribute(java.lang.String namespace, java.lang.String attName,
                                             javax.xml.namespace.QName qname, javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {

                java.lang.String attributeNamespace = qname.getNamespaceURI();
                java.lang.String attributePrefix = xmlWriter.getPrefix(attributeNamespace);
                if (attributePrefix == null) {
                    attributePrefix = registerPrefix(xmlWriter, attributeNamespace);
                }
                java.lang.String attributeValue;
                if (attributePrefix.trim().length() > 0) {
                    attributeValue = attributePrefix + ":" + qname.getLocalPart();
                } else {
                    attributeValue = qname.getLocalPart();
                }

                if (namespace.equals("")) {
                    xmlWriter.writeAttribute(attName, attributeValue);
                } else {
                    registerPrefix(xmlWriter, namespace);
                    xmlWriter.writeAttribute(namespace, attName, attributeValue);
                }
            }
        /**
         *  method to handle Qnames
         */

        private void writeQName(javax.xml.namespace.QName qname,
                                javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {
            java.lang.String namespaceURI = qname.getNamespaceURI();
            if (namespaceURI != null) {
                java.lang.String prefix = xmlWriter.getPrefix(namespaceURI);
                if (prefix == null) {
                    prefix = generatePrefix(namespaceURI);
                    xmlWriter.writeNamespace(prefix, namespaceURI);
                    xmlWriter.setPrefix(prefix,namespaceURI);
                }

                if (prefix.trim().length() > 0){
                    xmlWriter.writeCharacters(prefix + ":" + org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
                } else {
                    // i.e this is the default namespace
                    xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
                }

            } else {
                xmlWriter.writeCharacters(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qname));
            }
        }

        private void writeQNames(javax.xml.namespace.QName[] qnames,
                                 javax.xml.stream.XMLStreamWriter xmlWriter) throws javax.xml.stream.XMLStreamException {

            if (qnames != null) {
                // we have to store this data until last moment since it is not possible to write any
                // namespace data after writing the charactor data
                java.lang.StringBuffer stringToWrite = new java.lang.StringBuffer();
                java.lang.String namespaceURI = null;
                java.lang.String prefix = null;

                for (int i = 0; i < qnames.length; i++) {
                    if (i > 0) {
                        stringToWrite.append(" ");
                    }
                    namespaceURI = qnames[i].getNamespaceURI();
                    if (namespaceURI != null) {
                        prefix = xmlWriter.getPrefix(namespaceURI);
                        if ((prefix == null) || (prefix.length() == 0)) {
                            prefix = generatePrefix(namespaceURI);
                            xmlWriter.writeNamespace(prefix, namespaceURI);
                            xmlWriter.setPrefix(prefix,namespaceURI);
                        }

                        if (prefix.trim().length() > 0){
                            stringToWrite.append(prefix).append(":").append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                        } else {
                            stringToWrite.append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                        }
                    } else {
                        stringToWrite.append(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(qnames[i]));
                    }
                }
                xmlWriter.writeCharacters(stringToWrite.toString());
            }

        }


         /**
         * Register a namespace prefix
         */
         private java.lang.String registerPrefix(javax.xml.stream.XMLStreamWriter xmlWriter, java.lang.String namespace) throws javax.xml.stream.XMLStreamException {
                java.lang.String prefix = xmlWriter.getPrefix(namespace);

                if (prefix == null) {
                    prefix = generatePrefix(namespace);

                    while (xmlWriter.getNamespaceContext().getNamespaceURI(prefix) != null) {
                        prefix = org.apache.axis2.databinding.utils.BeanUtil.getUniquePrefix();
                    }

                    xmlWriter.writeNamespace(prefix, namespace);
                    xmlWriter.setPrefix(prefix, namespace);
                }

                return prefix;
            }


  
        /**
        * databinding method to get an XML representation of this object
        *
        */
        public javax.xml.stream.XMLStreamReader getPullParser(javax.xml.namespace.QName qName)
                    throws org.apache.axis2.databinding.ADBException{


        
                 java.util.ArrayList elementList = new java.util.ArrayList();
                 java.util.ArrayList attribList = new java.util.ArrayList();

                 if (localPublisherNameTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "publisherName"));
                                 
                                        if (localPublisherName != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localPublisherName));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("publisherName cannot be null!!");
                                        }
                                    } if (localSensorNameTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "sensorName"));
                                 
                                        if (localSensorName != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSensorName));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("sensorName cannot be null!!");
                                        }
                                    }
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "longitude"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localLongitude));
                            
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "latitude"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localLatitude));
                            
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "altitude"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localAltitude));
                             if (localUnitTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "unit"));
                                 
                                        if (localUnit != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUnit));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("unit cannot be null!!");
                                        }
                                    } if (localSensorTypeTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "sensorType"));
                                 
                                        if (localSensorType != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSensorType));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("sensorType cannot be null!!");
                                        }
                                    } if (localUrlTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "url"));
                                 
                                        if (localUrl != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localUrl));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("url cannot be null!!");
                                        }
                                    } if (localKeywordsTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "keywords"));
                                 
                                        if (localKeywords != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localKeywords));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("keywords cannot be null!!");
                                        }
                                    } if (localDescriptionTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "description"));
                                 
                                        if (localDescription != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDescription));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("description cannot be null!!");
                                        }
                                    } if (localDataTypeTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "dataType"));
                                 
                                        if (localDataType != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localDataType));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("dataType cannot be null!!");
                                        }
                                    }
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "samplingPeriod"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localSamplingPeriod));
                            
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "reportPeriod"));
                                 
                                elementList.add(
                                   org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localReportPeriod));
                            
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "entryTime"));
                                 
                                        if (localEntryTime != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localEntryTime));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("entryTime cannot be null!!");
                                        }
                                     if (localWebServiceUrlTracker){
                                      elementList.add(new javax.xml.namespace.QName("http://tempuri.org/",
                                                                      "webServiceUrl"));
                                 
                                        if (localWebServiceUrl != null){
                                            elementList.add(org.apache.axis2.databinding.utils.ConverterUtil.convertToString(localWebServiceUrl));
                                        } else {
                                           throw new org.apache.axis2.databinding.ADBException("webServiceUrl cannot be null!!");
                                        }
                                    }

                return new org.apache.axis2.databinding.utils.reader.ADBXMLStreamReaderImpl(qName, elementList.toArray(), attribList.toArray());
            
            

        }

  

     /**
      *  Factory class that keeps the parse method
      */
    public static class Factory{

        
        

        /**
        * static method to create the object
        * Precondition:  If this object is an element, the current or next start element starts this object and any intervening reader events are ignorable
        *                If this object is not an element, it is a complex type and the reader is at the event just after the outer start element
        * Postcondition: If this object is an element, the reader is positioned at its end element
        *                If this object is a complex type, the reader is positioned at the end element of its outer element
        */
        public static SensorInfo parse(javax.xml.stream.XMLStreamReader reader) throws java.lang.Exception{
            SensorInfo object =
                new SensorInfo();

            int event;
            java.lang.String nillableValue = null;
            java.lang.String prefix ="";
            java.lang.String namespaceuri ="";
            try {
                
                while (!reader.isStartElement() && !reader.isEndElement())
                    reader.next();

                
                if (reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance","type")!=null){
                  java.lang.String fullTypeName = reader.getAttributeValue("http://www.w3.org/2001/XMLSchema-instance",
                        "type");
                  if (fullTypeName!=null){
                    java.lang.String nsPrefix = null;
                    if (fullTypeName.indexOf(":") > -1){
                        nsPrefix = fullTypeName.substring(0,fullTypeName.indexOf(":"));
                    }
                    nsPrefix = nsPrefix==null?"":nsPrefix;

                    java.lang.String type = fullTypeName.substring(fullTypeName.indexOf(":")+1);
                    
                            if (!"SensorInfo".equals(type)){
                                //find namespace for the prefix
                                java.lang.String nsUri = reader.getNamespaceContext().getNamespaceURI(nsPrefix);
                                return (SensorInfo)org.tempuri.ExtensionMapper.getTypeObject(
                                     nsUri,type,reader);
                              }
                        

                  }
                

                }

                

                
                // Note all attributes that were handled. Used to differ normal attributes
                // from anyAttributes.
                java.util.Vector handledAttributes = new java.util.Vector();
                

                 
                    
                    reader.next();
                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","publisherName").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setPublisherName(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","sensorName").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSensorName(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","longitude").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setLongitude(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDouble(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","latitude").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setLatitude(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDouble(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","altitude").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setAltitude(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDouble(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","unit").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setUnit(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","sensorType").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSensorType(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","url").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setUrl(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","keywords").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setKeywords(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","description").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDescription(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","dataType").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setDataType(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","samplingPeriod").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setSamplingPeriod(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDouble(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","reportPeriod").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setReportPeriod(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDouble(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","entryTime").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setEntryTime(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToDateTime(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                else{
                                    // A start element we are not expecting indicates an invalid parameter was passed
                                    throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                                }
                            
                                    
                                    while (!reader.isStartElement() && !reader.isEndElement()) reader.next();
                                
                                    if (reader.isStartElement() && new javax.xml.namespace.QName("http://tempuri.org/","webServiceUrl").equals(reader.getName())){
                                
                                    java.lang.String content = reader.getElementText();
                                    
                                              object.setWebServiceUrl(
                                                    org.apache.axis2.databinding.utils.ConverterUtil.convertToString(content));
                                              
                                        reader.next();
                                    
                              }  // End of if for expected property start element
                                
                                    else {
                                        
                                    }
                                  
                            while (!reader.isStartElement() && !reader.isEndElement())
                                reader.next();
                            
                                if (reader.isStartElement())
                                // A start element we are not expecting indicates a trailing invalid property
                                throw new org.apache.axis2.databinding.ADBException("Unexpected subelement " + reader.getLocalName());
                            



            } catch (javax.xml.stream.XMLStreamException e) {
                throw new java.lang.Exception(e);
            }

            return object;
        }

        }//end of factory class

        

        }
           
          