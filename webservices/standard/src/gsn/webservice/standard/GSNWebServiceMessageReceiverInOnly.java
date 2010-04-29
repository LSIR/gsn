

/**
 * GSNWebServiceMessageReceiverInOnly.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:35 LKT)
 */
        package gsn.webservice.standard;

        /**
        *  GSNWebServiceMessageReceiverInOnly message receiver
        */

        public class GSNWebServiceMessageReceiverInOnly extends org.apache.axis2.receivers.AbstractInMessageReceiver{

        public void invokeBusinessLogic(org.apache.axis2.context.MessageContext inMessage) throws org.apache.axis2.AxisFault{

        try {

        // get the implementation class for the Web Service
        Object obj = getTheImplementationObject(inMessage);

        GSNWebServiceSkeleton skel = (GSNWebServiceSkeleton)obj;
        //Out Envelop
        org.apache.axiom.soap.SOAPEnvelope envelope = null;
        //Find the axisOperation that has been set by the Dispatch phase.
        org.apache.axis2.description.AxisOperation op = inMessage.getOperationContext().getAxisOperation();
        if (op == null) {
        throw new org.apache.axis2.AxisFault("Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
        }

        java.lang.String methodName;
        if((op.getName() != null) && ((methodName = org.apache.axis2.util.JavaUtils.xmlNameToJava(op.getName().getLocalPart())) != null)){

        
            if("unregisterQuery".equals(methodName)){
            
            gsn.webservice.standard.UnregisterQuery wrappedParam = (gsn.webservice.standard.UnregisterQuery)fromOM(
                                                        inMessage.getEnvelope().getBody().getFirstElement(),
                                                        gsn.webservice.standard.UnregisterQuery.class,
                                                        getEnvelopeNamespaces(inMessage.getEnvelope()));
                                            
                                                     skel.unregisterQuery(wrappedParam);
                                                } else 
            if("createVirtualSensor".equals(methodName)){
            
            gsn.webservice.standard.CreateVirtualSensor wrappedParam = (gsn.webservice.standard.CreateVirtualSensor)fromOM(
                                                        inMessage.getEnvelope().getBody().getFirstElement(),
                                                        gsn.webservice.standard.CreateVirtualSensor.class,
                                                        getEnvelopeNamespaces(inMessage.getEnvelope()));
                                            
                                                     skel.createVirtualSensor(wrappedParam);
                                                } else 
            if("registerQuery".equals(methodName)){
            
            gsn.webservice.standard.RegisterQuery wrappedParam = (gsn.webservice.standard.RegisterQuery)fromOM(
                                                        inMessage.getEnvelope().getBody().getFirstElement(),
                                                        gsn.webservice.standard.RegisterQuery.class,
                                                        getEnvelopeNamespaces(inMessage.getEnvelope()));
                                            
                                                     skel.registerQuery(wrappedParam);
                                                } else 
            if("deleteVirtualSensor".equals(methodName)){
            
            gsn.webservice.standard.DeleteVirtualSensor wrappedParam = (gsn.webservice.standard.DeleteVirtualSensor)fromOM(
                                                        inMessage.getEnvelope().getBody().getFirstElement(),
                                                        gsn.webservice.standard.DeleteVirtualSensor.class,
                                                        getEnvelopeNamespaces(inMessage.getEnvelope()));
                                            
                                                     skel.deleteVirtualSensor(wrappedParam);
                                                
                } else {
                  throw new java.lang.RuntimeException("method not found");
                }
            

        }
        } catch (java.lang.Exception e) {
        throw org.apache.axis2.AxisFault.makeFault(e);
        }
        }


        
        //
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetVirtualSensorsDetails param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetVirtualSensorsDetails.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetVirtualSensorsDetailsResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetVirtualSensorsDetailsResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetNextData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetNextData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetNextDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetNextDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.ListWrapperURLsResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.ListWrapperURLsResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetLatestMultiData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetLatestMultiData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetLatestMultiDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetLatestMultiDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.UnregisterQuery param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.UnregisterQuery.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.CreateVirtualSensor param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.CreateVirtualSensor.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetMultiData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetMultiData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetMultiDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetMultiDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.RegisterQuery param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.RegisterQuery.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetVirtualSensorDetails param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetVirtualSensorDetails.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetVirtualSensorDetailsResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetVirtualSensorDetailsResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.GetContainerInfoResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.GetContainerInfoResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.ListVirtualSensorNamesResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.ListVirtualSensorNamesResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.webservice.standard.DeleteVirtualSensor param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.webservice.standard.DeleteVirtualSensor.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.GetVirtualSensorsDetailsResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.GetVirtualSensorsDetailsResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.GetVirtualSensorsDetailsResponse wrapgetVirtualSensorsDetails(){
                                gsn.webservice.standard.GetVirtualSensorsDetailsResponse wrappedElement = new gsn.webservice.standard.GetVirtualSensorsDetailsResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.GetNextDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.GetNextDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.GetNextDataResponse wrapgetNextData(){
                                gsn.webservice.standard.GetNextDataResponse wrappedElement = new gsn.webservice.standard.GetNextDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.ListWrapperURLsResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.ListWrapperURLsResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.ListWrapperURLsResponse wraplistWrapperURLs(){
                                gsn.webservice.standard.ListWrapperURLsResponse wrappedElement = new gsn.webservice.standard.ListWrapperURLsResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.GetLatestMultiDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.GetLatestMultiDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.GetLatestMultiDataResponse wrapgetLatestMultiData(){
                                gsn.webservice.standard.GetLatestMultiDataResponse wrappedElement = new gsn.webservice.standard.GetLatestMultiDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.GetMultiDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.GetMultiDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.GetMultiDataResponse wrapgetMultiData(){
                                gsn.webservice.standard.GetMultiDataResponse wrappedElement = new gsn.webservice.standard.GetMultiDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.GetVirtualSensorDetailsResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.GetVirtualSensorDetailsResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.GetVirtualSensorDetailsResponse wrapgetVirtualSensorDetails(){
                                gsn.webservice.standard.GetVirtualSensorDetailsResponse wrappedElement = new gsn.webservice.standard.GetVirtualSensorDetailsResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.GetContainerInfoResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.GetContainerInfoResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.GetContainerInfoResponse wrapgetContainerInfo(){
                                gsn.webservice.standard.GetContainerInfoResponse wrappedElement = new gsn.webservice.standard.GetContainerInfoResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.webservice.standard.ListVirtualSensorNamesResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.webservice.standard.ListVirtualSensorNamesResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.webservice.standard.ListVirtualSensorNamesResponse wraplistVirtualSensorNames(){
                                gsn.webservice.standard.ListVirtualSensorNamesResponse wrappedElement = new gsn.webservice.standard.ListVirtualSensorNamesResponse();
                                return wrappedElement;
                         }
                    


        /**
        *  get the default envelope
        */
        private org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory){
        return factory.getDefaultEnvelope();
        }


        private  java.lang.Object fromOM(
        org.apache.axiom.om.OMElement param,
        java.lang.Class type,
        java.util.Map extraNamespaces) throws org.apache.axis2.AxisFault{

        try {
        
                if (gsn.webservice.standard.GetVirtualSensorsDetails.class.equals(type)){
                
                           return gsn.webservice.standard.GetVirtualSensorsDetails.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetVirtualSensorsDetailsResponse.class.equals(type)){
                
                           return gsn.webservice.standard.GetVirtualSensorsDetailsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetNextData.class.equals(type)){
                
                           return gsn.webservice.standard.GetNextData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetNextDataResponse.class.equals(type)){
                
                           return gsn.webservice.standard.GetNextDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.ListWrapperURLsResponse.class.equals(type)){
                
                           return gsn.webservice.standard.ListWrapperURLsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetLatestMultiData.class.equals(type)){
                
                           return gsn.webservice.standard.GetLatestMultiData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetLatestMultiDataResponse.class.equals(type)){
                
                           return gsn.webservice.standard.GetLatestMultiDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.UnregisterQuery.class.equals(type)){
                
                           return gsn.webservice.standard.UnregisterQuery.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.CreateVirtualSensor.class.equals(type)){
                
                           return gsn.webservice.standard.CreateVirtualSensor.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetMultiData.class.equals(type)){
                
                           return gsn.webservice.standard.GetMultiData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetMultiDataResponse.class.equals(type)){
                
                           return gsn.webservice.standard.GetMultiDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.RegisterQuery.class.equals(type)){
                
                           return gsn.webservice.standard.RegisterQuery.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetVirtualSensorDetails.class.equals(type)){
                
                           return gsn.webservice.standard.GetVirtualSensorDetails.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetVirtualSensorDetailsResponse.class.equals(type)){
                
                           return gsn.webservice.standard.GetVirtualSensorDetailsResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.GetContainerInfoResponse.class.equals(type)){
                
                           return gsn.webservice.standard.GetContainerInfoResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.ListVirtualSensorNamesResponse.class.equals(type)){
                
                           return gsn.webservice.standard.ListVirtualSensorNamesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.webservice.standard.DeleteVirtualSensor.class.equals(type)){
                
                           return gsn.webservice.standard.DeleteVirtualSensor.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
        } catch (java.lang.Exception e) {
        throw org.apache.axis2.AxisFault.makeFault(e);
        }
           return null;
        }



    



        /**
        *  A utility method that copies the namepaces from the SOAPEnvelope
        */
        private java.util.Map getEnvelopeNamespaces(org.apache.axiom.soap.SOAPEnvelope env){
        java.util.Map returnMap = new java.util.HashMap();
        java.util.Iterator namespaceIterator = env.getAllDeclaredNamespaces();
        while (namespaceIterator.hasNext()) {
        org.apache.axiom.om.OMNamespace ns = (org.apache.axiom.om.OMNamespace) namespaceIterator.next();
        returnMap.put(ns.getPrefix(),ns.getNamespaceURI());
        }
        return returnMap;
        }



        }//end of class

    