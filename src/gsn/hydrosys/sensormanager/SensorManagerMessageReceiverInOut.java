
/**
 * SensorManagerMessageReceiverInOut.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:35 LKT)
 */
        package gsn.hydrosys.sensormanager;

        /**
        *  SensorManagerMessageReceiverInOut message receiver
        */

        public class SensorManagerMessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutMessageReceiver{


        public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext, org.apache.axis2.context.MessageContext newMsgContext)
        throws org.apache.axis2.AxisFault{

        try {

        // get the implementation class for the Web Service
        Object obj = getTheImplementationObject(msgContext);

        SensorManagerSkeleton skel = (SensorManagerSkeleton)obj;
        //Out Envelop
        org.apache.axiom.soap.SOAPEnvelope envelope = null;
        //Find the axisOperation that has been set by the Dispatch phase.
        org.apache.axis2.description.AxisOperation op = msgContext.getOperationContext().getAxisOperation();
        if (op == null) {
        throw new org.apache.axis2.AxisFault("Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
        }

        java.lang.String methodName;
        if((op.getName() != null) && ((methodName = org.apache.axis2.util.JavaUtils.xmlNameToJava(op.getName().getLocalPart())) != null)){

        

            if("registerQuery".equals(methodName)){
                
                gsn.hydrosys.sensormanager.RegisterQueryResponse registerQueryResponse1 = null;
	                        gsn.hydrosys.sensormanager.RegisterQuery wrappedParam =
                                                             (gsn.hydrosys.sensormanager.RegisterQuery)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    gsn.hydrosys.sensormanager.RegisterQuery.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               registerQueryResponse1 =
                                                   
                                                   
                                                         skel.registerQuery(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), registerQueryResponse1, false);
                                    } else 

            if("unregisterQuery".equals(methodName)){
                
                gsn.hydrosys.sensormanager.UnregisterQueryResponse unregisterQueryResponse3 = null;
	                        gsn.hydrosys.sensormanager.UnregisterQuery wrappedParam =
                                                             (gsn.hydrosys.sensormanager.UnregisterQuery)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    gsn.hydrosys.sensormanager.UnregisterQuery.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               unregisterQueryResponse3 =
                                                   
                                                   
                                                         skel.unregisterQuery(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), unregisterQueryResponse3, false);
                                    
            } else {
              throw new java.lang.RuntimeException("method not found");
            }
        

        newMsgContext.setEnvelope(envelope);
        }
        }
        catch (java.lang.Exception e) {
        throw org.apache.axis2.AxisFault.makeFault(e);
        }
        }
        
        //
            private  org.apache.axiom.om.OMElement  toOM(gsn.hydrosys.sensormanager.RegisterQuery param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.hydrosys.sensormanager.RegisterQuery.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.hydrosys.sensormanager.RegisterQueryResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.hydrosys.sensormanager.RegisterQueryResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.hydrosys.sensormanager.UnregisterQuery param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.hydrosys.sensormanager.UnregisterQuery.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(gsn.hydrosys.sensormanager.UnregisterQueryResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(gsn.hydrosys.sensormanager.UnregisterQueryResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.hydrosys.sensormanager.RegisterQueryResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.hydrosys.sensormanager.RegisterQueryResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.hydrosys.sensormanager.RegisterQueryResponse wrapregisterQuery(){
                                gsn.hydrosys.sensormanager.RegisterQueryResponse wrappedElement = new gsn.hydrosys.sensormanager.RegisterQueryResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, gsn.hydrosys.sensormanager.UnregisterQueryResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(gsn.hydrosys.sensormanager.UnregisterQueryResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private gsn.hydrosys.sensormanager.UnregisterQueryResponse wrapunregisterQuery(){
                                gsn.hydrosys.sensormanager.UnregisterQueryResponse wrappedElement = new gsn.hydrosys.sensormanager.UnregisterQueryResponse();
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
        
                if (gsn.hydrosys.sensormanager.RegisterQuery.class.equals(type)){
                
                           return gsn.hydrosys.sensormanager.RegisterQuery.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.hydrosys.sensormanager.RegisterQueryResponse.class.equals(type)){
                
                           return gsn.hydrosys.sensormanager.RegisterQueryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.hydrosys.sensormanager.UnregisterQuery.class.equals(type)){
                
                           return gsn.hydrosys.sensormanager.UnregisterQuery.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (gsn.hydrosys.sensormanager.UnregisterQueryResponse.class.equals(type)){
                
                           return gsn.hydrosys.sensormanager.UnregisterQueryResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

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

        private org.apache.axis2.AxisFault createAxisFault(java.lang.Exception e) {
        org.apache.axis2.AxisFault f;
        Throwable cause = e.getCause();
        if (cause != null) {
            f = new org.apache.axis2.AxisFault(e.getMessage(), cause);
        } else {
            f = new org.apache.axis2.AxisFault(e.getMessage());
        }

        return f;
    }

        }//end of class
    