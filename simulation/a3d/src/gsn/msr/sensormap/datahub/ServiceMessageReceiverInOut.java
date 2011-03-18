
/**
 * ServiceMessageReceiverInOut.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:35 LKT)
 */
        package gsn.msr.sensormap.datahub;

        /**
        *  ServiceMessageReceiverInOut message receiver
        */

        public class ServiceMessageReceiverInOut extends org.apache.axis2.receivers.AbstractInOutMessageReceiver{


        public void invokeBusinessLogic(org.apache.axis2.context.MessageContext msgContext, org.apache.axis2.context.MessageContext newMsgContext)
        throws org.apache.axis2.AxisFault{

        try {

        // get the implementation class for the Web Service
        Object obj = getTheImplementationObject(msgContext);

        ServiceSkeleton skel = (ServiceSkeleton)obj;
        //Out Envelop
        org.apache.axiom.soap.SOAPEnvelope envelope = null;
        //Find the axisOperation that has been set by the Dispatch phase.
        org.apache.axis2.description.AxisOperation op = msgContext.getOperationContext().getAxisOperation();
        if (op == null) {
        throw new org.apache.axis2.AxisFault("Operation is not located, if this is doclit style the SOAP-ACTION should specified via the SOAP Action to use the RawXMLProvider");
        }

        java.lang.String methodName;
        if((op.getName() != null) && ((methodName = org.apache.axis2.util.JavaUtils.xmlNameToJava(op.getName().getLocalPart())) != null)){

        

            if("DeleteSensor".equals(methodName)){
                
                org.tempuri.DeleteSensorResponse deleteSensorResponse1 = null;
	                        org.tempuri.DeleteSensor wrappedParam =
                                                             (org.tempuri.DeleteSensor)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.DeleteSensor.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               deleteSensorResponse1 =
                                                   
                                                   
                                                         skel.DeleteSensor(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), deleteSensorResponse1, false);
                                    } else 

            if("GetAggregateScalarDataSeriesInBatch".equals(methodName)){
                
                org.tempuri.GetAggregateScalarDataSeriesInBatchResponse getAggregateScalarDataSeriesInBatchResponse3 = null;
	                        org.tempuri.GetAggregateScalarDataSeriesInBatch wrappedParam =
                                                             (org.tempuri.GetAggregateScalarDataSeriesInBatch)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetAggregateScalarDataSeriesInBatch.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getAggregateScalarDataSeriesInBatchResponse3 =
                                                   
                                                   
                                                         skel.GetAggregateScalarDataSeriesInBatch(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getAggregateScalarDataSeriesInBatchResponse3, false);
                                    } else 

            if("StoreScalarDataBatch".equals(methodName)){
                
                org.tempuri.StoreScalarDataBatchResponse storeScalarDataBatchResponse5 = null;
	                        org.tempuri.StoreScalarDataBatch wrappedParam =
                                                             (org.tempuri.StoreScalarDataBatch)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.StoreScalarDataBatch.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               storeScalarDataBatchResponse5 =
                                                   
                                                   
                                                         skel.StoreScalarDataBatch(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), storeScalarDataBatchResponse5, false);
                                    } else 

            if("GetLatestVectorDataByComponentIndex".equals(methodName)){
                
                org.tempuri.GetLatestVectorDataByComponentIndexResponse getLatestVectorDataByComponentIndexResponse7 = null;
	                        org.tempuri.GetLatestVectorDataByComponentIndex wrappedParam =
                                                             (org.tempuri.GetLatestVectorDataByComponentIndex)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetLatestVectorDataByComponentIndex.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getLatestVectorDataByComponentIndexResponse7 =
                                                   
                                                   
                                                         skel.GetLatestVectorDataByComponentIndex(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getLatestVectorDataByComponentIndexResponse7, false);
                                    } else 

            if("GetLatestBinarySensorData".equals(methodName)){
                
                org.tempuri.GetLatestBinarySensorDataResponse getLatestBinarySensorDataResponse9 = null;
	                        org.tempuri.GetLatestBinarySensorData wrappedParam =
                                                             (org.tempuri.GetLatestBinarySensorData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetLatestBinarySensorData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getLatestBinarySensorDataResponse9 =
                                                   
                                                   
                                                         skel.GetLatestBinarySensorData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getLatestBinarySensorDataResponse9, false);
                                    } else 

            if("UpdateSensorLocation".equals(methodName)){
                
                org.tempuri.UpdateSensorLocationResponse updateSensorLocationResponse11 = null;
	                        org.tempuri.UpdateSensorLocation wrappedParam =
                                                             (org.tempuri.UpdateSensorLocation)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.UpdateSensorLocation.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               updateSensorLocationResponse11 =
                                                   
                                                   
                                                         skel.UpdateSensorLocation(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), updateSensorLocationResponse11, false);
                                    } else 

            if("GetLatestScalarData".equals(methodName)){
                
                org.tempuri.GetLatestScalarDataResponse getLatestScalarDataResponse13 = null;
	                        org.tempuri.GetLatestScalarData wrappedParam =
                                                             (org.tempuri.GetLatestScalarData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetLatestScalarData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getLatestScalarDataResponse13 =
                                                   
                                                   
                                                         skel.GetLatestScalarData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getLatestScalarDataResponse13, false);
                                    } else 

            if("RegisterSensor".equals(methodName)){
                
                org.tempuri.RegisterSensorResponse registerSensorResponse15 = null;
	                        org.tempuri.RegisterSensor wrappedParam =
                                                             (org.tempuri.RegisterSensor)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.RegisterSensor.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               registerSensorResponse15 =
                                                   
                                                   
                                                         skel.RegisterSensor(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), registerSensorResponse15, false);
                                    } else 

            if("GetAggregateScalarDataInBatch".equals(methodName)){
                
                org.tempuri.GetAggregateScalarDataInBatchResponse getAggregateScalarDataInBatchResponse17 = null;
	                        org.tempuri.GetAggregateScalarDataInBatch wrappedParam =
                                                             (org.tempuri.GetAggregateScalarDataInBatch)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetAggregateScalarDataInBatch.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getAggregateScalarDataInBatchResponse17 =
                                                   
                                                   
                                                         skel.GetAggregateScalarDataInBatch(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getAggregateScalarDataInBatchResponse17, false);
                                    } else 

            if("StoreVectorData".equals(methodName)){
                
                org.tempuri.StoreVectorDataResponse storeVectorDataResponse19 = null;
	                        org.tempuri.StoreVectorData wrappedParam =
                                                             (org.tempuri.StoreVectorData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.StoreVectorData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               storeVectorDataResponse19 =
                                                   
                                                   
                                                         skel.StoreVectorData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), storeVectorDataResponse19, false);
                                    } else 

            if("GetSensorByPublisherAndName".equals(methodName)){
                
                org.tempuri.GetSensorByPublisherAndNameResponse getSensorByPublisherAndNameResponse21 = null;
	                        org.tempuri.GetSensorByPublisherAndName wrappedParam =
                                                             (org.tempuri.GetSensorByPublisherAndName)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetSensorByPublisherAndName.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getSensorByPublisherAndNameResponse21 =
                                                   
                                                   
                                                         skel.GetSensorByPublisherAndName(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getSensorByPublisherAndNameResponse21, false);
                                    } else 

            if("GetLatestVectorData".equals(methodName)){
                
                org.tempuri.GetLatestVectorDataResponse getLatestVectorDataResponse23 = null;
	                        org.tempuri.GetLatestVectorData wrappedParam =
                                                             (org.tempuri.GetLatestVectorData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetLatestVectorData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getLatestVectorDataResponse23 =
                                                   
                                                   
                                                         skel.GetLatestVectorData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getLatestVectorDataResponse23, false);
                                    } else 

            if("GetScalarDataSeriesInBatch".equals(methodName)){
                
                org.tempuri.GetScalarDataSeriesInBatchResponse getScalarDataSeriesInBatchResponse25 = null;
	                        org.tempuri.GetScalarDataSeriesInBatch wrappedParam =
                                                             (org.tempuri.GetScalarDataSeriesInBatch)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetScalarDataSeriesInBatch.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getScalarDataSeriesInBatchResponse25 =
                                                   
                                                   
                                                         skel.GetScalarDataSeriesInBatch(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getScalarDataSeriesInBatchResponse25, false);
                                    } else 

            if("GetAggregateScalarData".equals(methodName)){
                
                org.tempuri.GetAggregateScalarDataResponse getAggregateScalarDataResponse27 = null;
	                        org.tempuri.GetAggregateScalarData wrappedParam =
                                                             (org.tempuri.GetAggregateScalarData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetAggregateScalarData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getAggregateScalarDataResponse27 =
                                                   
                                                   
                                                         skel.GetAggregateScalarData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getAggregateScalarDataResponse27, false);
                                    } else 

            if("SetSensorData".equals(methodName)){
                
                org.tempuri.SetSensorDataResponse setSensorDataResponse29 = null;
	                        org.tempuri.SetSensorData wrappedParam =
                                                             (org.tempuri.SetSensorData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.SetSensorData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               setSensorDataResponse29 =
                                                   
                                                   
                                                         skel.SetSensorData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), setSensorDataResponse29, false);
                                    } else 

            if("StoreBinaryData".equals(methodName)){
                
                org.tempuri.StoreBinaryDataResponse storeBinaryDataResponse31 = null;
	                        org.tempuri.StoreBinaryData wrappedParam =
                                                             (org.tempuri.StoreBinaryData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.StoreBinaryData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               storeBinaryDataResponse31 =
                                                   
                                                   
                                                         skel.StoreBinaryData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), storeBinaryDataResponse31, false);
                                    } else 

            if("GetScalarDataSeries".equals(methodName)){
                
                org.tempuri.GetScalarDataSeriesResponse getScalarDataSeriesResponse33 = null;
	                        org.tempuri.GetScalarDataSeries wrappedParam =
                                                             (org.tempuri.GetScalarDataSeries)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetScalarDataSeries.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getScalarDataSeriesResponse33 =
                                                   
                                                   
                                                         skel.GetScalarDataSeries(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getScalarDataSeriesResponse33, false);
                                    } else 

            if("GetSensorsByPublisher".equals(methodName)){
                
                org.tempuri.GetSensorsByPublisherResponse getSensorsByPublisherResponse35 = null;
	                        org.tempuri.GetSensorsByPublisher wrappedParam =
                                                             (org.tempuri.GetSensorsByPublisher)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetSensorsByPublisher.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getSensorsByPublisherResponse35 =
                                                   
                                                   
                                                         skel.GetSensorsByPublisher(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getSensorsByPublisherResponse35, false);
                                    } else 

            if("GetLatestScalarDataInBatch".equals(methodName)){
                
                org.tempuri.GetLatestScalarDataInBatchResponse getLatestScalarDataInBatchResponse37 = null;
	                        org.tempuri.GetLatestScalarDataInBatch wrappedParam =
                                                             (org.tempuri.GetLatestScalarDataInBatch)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetLatestScalarDataInBatch.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getLatestScalarDataInBatchResponse37 =
                                                   
                                                   
                                                         skel.GetLatestScalarDataInBatch(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getLatestScalarDataInBatchResponse37, false);
                                    } else 

            if("DataToString".equals(methodName)){
                
                org.tempuri.DataToStringResponse dataToStringResponse39 = null;
	                        org.tempuri.DataToString wrappedParam =
                                                             (org.tempuri.DataToString)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.DataToString.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               dataToStringResponse39 =
                                                   
                                                   
                                                         skel.DataToString(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), dataToStringResponse39, false);
                                    } else 

            if("StoreVectorDataByComponentIndex".equals(methodName)){
                
                org.tempuri.StoreVectorDataByComponentIndexResponse storeVectorDataByComponentIndexResponse41 = null;
	                        org.tempuri.StoreVectorDataByComponentIndex wrappedParam =
                                                             (org.tempuri.StoreVectorDataByComponentIndex)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.StoreVectorDataByComponentIndex.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               storeVectorDataByComponentIndexResponse41 =
                                                   
                                                   
                                                         skel.StoreVectorDataByComponentIndex(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), storeVectorDataByComponentIndexResponse41, false);
                                    } else 

            if("RegisterVectorSensor".equals(methodName)){
                
                org.tempuri.RegisterVectorSensorResponse registerVectorSensorResponse43 = null;
	                        org.tempuri.RegisterVectorSensor wrappedParam =
                                                             (org.tempuri.RegisterVectorSensor)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.RegisterVectorSensor.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               registerVectorSensorResponse43 =
                                                   
                                                   
                                                         skel.RegisterVectorSensor(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), registerVectorSensorResponse43, false);
                                    } else 

            if("StoreScalarData".equals(methodName)){
                
                org.tempuri.StoreScalarDataResponse storeScalarDataResponse45 = null;
	                        org.tempuri.StoreScalarData wrappedParam =
                                                             (org.tempuri.StoreScalarData)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.StoreScalarData.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               storeScalarDataResponse45 =
                                                   
                                                   
                                                         skel.StoreScalarData(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), storeScalarDataResponse45, false);
                                    } else 

            if("DeleteVectorSensor".equals(methodName)){
                
                org.tempuri.DeleteVectorSensorResponse deleteVectorSensorResponse47 = null;
	                        org.tempuri.DeleteVectorSensor wrappedParam =
                                                             (org.tempuri.DeleteVectorSensor)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.DeleteVectorSensor.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               deleteVectorSensorResponse47 =
                                                   
                                                   
                                                         skel.DeleteVectorSensor(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), deleteVectorSensorResponse47, false);
                                    } else 

            if("GetAggregateScalarDataSeries".equals(methodName)){
                
                org.tempuri.GetAggregateScalarDataSeriesResponse getAggregateScalarDataSeriesResponse49 = null;
	                        org.tempuri.GetAggregateScalarDataSeries wrappedParam =
                                                             (org.tempuri.GetAggregateScalarDataSeries)fromOM(
                                    msgContext.getEnvelope().getBody().getFirstElement(),
                                    org.tempuri.GetAggregateScalarDataSeries.class,
                                    getEnvelopeNamespaces(msgContext.getEnvelope()));
                                                
                                               getAggregateScalarDataSeriesResponse49 =
                                                   
                                                   
                                                         skel.GetAggregateScalarDataSeries(wrappedParam)
                                                    ;
                                            
                                        envelope = toEnvelope(getSOAPFactory(msgContext), getAggregateScalarDataSeriesResponse49, false);
                                    
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
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.DeleteSensor param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.DeleteSensor.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.DeleteSensorResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.DeleteSensorResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarDataSeriesInBatch param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarDataSeriesInBatch.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarDataSeriesInBatchResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarDataSeriesInBatchResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreScalarDataBatch param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreScalarDataBatch.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreScalarDataBatchResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreScalarDataBatchResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestVectorDataByComponentIndex param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestVectorDataByComponentIndex.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestVectorDataByComponentIndexResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestVectorDataByComponentIndexResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestBinarySensorData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestBinarySensorData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestBinarySensorDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestBinarySensorDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.UpdateSensorLocation param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.UpdateSensorLocation.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.UpdateSensorLocationResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.UpdateSensorLocationResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestScalarData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestScalarData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestScalarDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestScalarDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.RegisterSensor param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.RegisterSensor.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.RegisterSensorResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.RegisterSensorResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarDataInBatch param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarDataInBatch.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarDataInBatchResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarDataInBatchResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreVectorData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreVectorData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreVectorDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreVectorDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetSensorByPublisherAndName param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetSensorByPublisherAndName.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetSensorByPublisherAndNameResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetSensorByPublisherAndNameResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestVectorData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestVectorData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestVectorDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestVectorDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetScalarDataSeriesInBatch param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetScalarDataSeriesInBatch.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetScalarDataSeriesInBatchResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetScalarDataSeriesInBatchResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.SetSensorData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.SetSensorData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.SetSensorDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.SetSensorDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreBinaryData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreBinaryData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreBinaryDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreBinaryDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetScalarDataSeries param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetScalarDataSeries.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetScalarDataSeriesResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetScalarDataSeriesResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetSensorsByPublisher param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetSensorsByPublisher.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetSensorsByPublisherResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetSensorsByPublisherResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestScalarDataInBatch param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestScalarDataInBatch.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetLatestScalarDataInBatchResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetLatestScalarDataInBatchResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.DataToString param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.DataToString.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.DataToStringResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.DataToStringResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreVectorDataByComponentIndex param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreVectorDataByComponentIndex.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreVectorDataByComponentIndexResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreVectorDataByComponentIndexResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.RegisterVectorSensor param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.RegisterVectorSensor.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.RegisterVectorSensorResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.RegisterVectorSensorResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreScalarData param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreScalarData.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.StoreScalarDataResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.StoreScalarDataResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.DeleteVectorSensor param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.DeleteVectorSensor.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.DeleteVectorSensorResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.DeleteVectorSensorResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarDataSeries param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarDataSeries.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
            private  org.apache.axiom.om.OMElement  toOM(org.tempuri.GetAggregateScalarDataSeriesResponse param, boolean optimizeContent)
            throws org.apache.axis2.AxisFault {

            
                        try{
                             return param.getOMElement(org.tempuri.GetAggregateScalarDataSeriesResponse.MY_QNAME,
                                          org.apache.axiom.om.OMAbstractFactory.getOMFactory());
                        } catch(org.apache.axis2.databinding.ADBException e){
                            throw org.apache.axis2.AxisFault.makeFault(e);
                        }
                    

            }
        
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.DeleteSensorResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.DeleteSensorResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.DeleteSensorResponse wrapDeleteSensor(){
                                org.tempuri.DeleteSensorResponse wrappedElement = new org.tempuri.DeleteSensorResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetAggregateScalarDataSeriesInBatchResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetAggregateScalarDataSeriesInBatchResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetAggregateScalarDataSeriesInBatchResponse wrapGetAggregateScalarDataSeriesInBatch(){
                                org.tempuri.GetAggregateScalarDataSeriesInBatchResponse wrappedElement = new org.tempuri.GetAggregateScalarDataSeriesInBatchResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.StoreScalarDataBatchResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.StoreScalarDataBatchResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.StoreScalarDataBatchResponse wrapStoreScalarDataBatch(){
                                org.tempuri.StoreScalarDataBatchResponse wrappedElement = new org.tempuri.StoreScalarDataBatchResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetLatestVectorDataByComponentIndexResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetLatestVectorDataByComponentIndexResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetLatestVectorDataByComponentIndexResponse wrapGetLatestVectorDataByComponentIndex(){
                                org.tempuri.GetLatestVectorDataByComponentIndexResponse wrappedElement = new org.tempuri.GetLatestVectorDataByComponentIndexResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetLatestBinarySensorDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetLatestBinarySensorDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetLatestBinarySensorDataResponse wrapGetLatestBinarySensorData(){
                                org.tempuri.GetLatestBinarySensorDataResponse wrappedElement = new org.tempuri.GetLatestBinarySensorDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.UpdateSensorLocationResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.UpdateSensorLocationResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.UpdateSensorLocationResponse wrapUpdateSensorLocation(){
                                org.tempuri.UpdateSensorLocationResponse wrappedElement = new org.tempuri.UpdateSensorLocationResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetLatestScalarDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetLatestScalarDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetLatestScalarDataResponse wrapGetLatestScalarData(){
                                org.tempuri.GetLatestScalarDataResponse wrappedElement = new org.tempuri.GetLatestScalarDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.RegisterSensorResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.RegisterSensorResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.RegisterSensorResponse wrapRegisterSensor(){
                                org.tempuri.RegisterSensorResponse wrappedElement = new org.tempuri.RegisterSensorResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetAggregateScalarDataInBatchResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetAggregateScalarDataInBatchResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetAggregateScalarDataInBatchResponse wrapGetAggregateScalarDataInBatch(){
                                org.tempuri.GetAggregateScalarDataInBatchResponse wrappedElement = new org.tempuri.GetAggregateScalarDataInBatchResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.StoreVectorDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.StoreVectorDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.StoreVectorDataResponse wrapStoreVectorData(){
                                org.tempuri.StoreVectorDataResponse wrappedElement = new org.tempuri.StoreVectorDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetSensorByPublisherAndNameResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetSensorByPublisherAndNameResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetSensorByPublisherAndNameResponse wrapGetSensorByPublisherAndName(){
                                org.tempuri.GetSensorByPublisherAndNameResponse wrappedElement = new org.tempuri.GetSensorByPublisherAndNameResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetLatestVectorDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetLatestVectorDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetLatestVectorDataResponse wrapGetLatestVectorData(){
                                org.tempuri.GetLatestVectorDataResponse wrappedElement = new org.tempuri.GetLatestVectorDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetScalarDataSeriesInBatchResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetScalarDataSeriesInBatchResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetScalarDataSeriesInBatchResponse wrapGetScalarDataSeriesInBatch(){
                                org.tempuri.GetScalarDataSeriesInBatchResponse wrappedElement = new org.tempuri.GetScalarDataSeriesInBatchResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetAggregateScalarDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetAggregateScalarDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetAggregateScalarDataResponse wrapGetAggregateScalarData(){
                                org.tempuri.GetAggregateScalarDataResponse wrappedElement = new org.tempuri.GetAggregateScalarDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.SetSensorDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.SetSensorDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.SetSensorDataResponse wrapSetSensorData(){
                                org.tempuri.SetSensorDataResponse wrappedElement = new org.tempuri.SetSensorDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.StoreBinaryDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.StoreBinaryDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.StoreBinaryDataResponse wrapStoreBinaryData(){
                                org.tempuri.StoreBinaryDataResponse wrappedElement = new org.tempuri.StoreBinaryDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetScalarDataSeriesResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetScalarDataSeriesResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetScalarDataSeriesResponse wrapGetScalarDataSeries(){
                                org.tempuri.GetScalarDataSeriesResponse wrappedElement = new org.tempuri.GetScalarDataSeriesResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetSensorsByPublisherResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetSensorsByPublisherResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetSensorsByPublisherResponse wrapGetSensorsByPublisher(){
                                org.tempuri.GetSensorsByPublisherResponse wrappedElement = new org.tempuri.GetSensorsByPublisherResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetLatestScalarDataInBatchResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetLatestScalarDataInBatchResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetLatestScalarDataInBatchResponse wrapGetLatestScalarDataInBatch(){
                                org.tempuri.GetLatestScalarDataInBatchResponse wrappedElement = new org.tempuri.GetLatestScalarDataInBatchResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.DataToStringResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.DataToStringResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.DataToStringResponse wrapDataToString(){
                                org.tempuri.DataToStringResponse wrappedElement = new org.tempuri.DataToStringResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.StoreVectorDataByComponentIndexResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.StoreVectorDataByComponentIndexResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.StoreVectorDataByComponentIndexResponse wrapStoreVectorDataByComponentIndex(){
                                org.tempuri.StoreVectorDataByComponentIndexResponse wrappedElement = new org.tempuri.StoreVectorDataByComponentIndexResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.RegisterVectorSensorResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.RegisterVectorSensorResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.RegisterVectorSensorResponse wrapRegisterVectorSensor(){
                                org.tempuri.RegisterVectorSensorResponse wrappedElement = new org.tempuri.RegisterVectorSensorResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.StoreScalarDataResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.StoreScalarDataResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.StoreScalarDataResponse wrapStoreScalarData(){
                                org.tempuri.StoreScalarDataResponse wrappedElement = new org.tempuri.StoreScalarDataResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.DeleteVectorSensorResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.DeleteVectorSensorResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.DeleteVectorSensorResponse wrapDeleteVectorSensor(){
                                org.tempuri.DeleteVectorSensorResponse wrappedElement = new org.tempuri.DeleteVectorSensorResponse();
                                return wrappedElement;
                         }
                    
                    private  org.apache.axiom.soap.SOAPEnvelope toEnvelope(org.apache.axiom.soap.SOAPFactory factory, org.tempuri.GetAggregateScalarDataSeriesResponse param, boolean optimizeContent)
                        throws org.apache.axis2.AxisFault{
                      try{
                          org.apache.axiom.soap.SOAPEnvelope emptyEnvelope = factory.getDefaultEnvelope();
                           
                                    emptyEnvelope.getBody().addChild(param.getOMElement(org.tempuri.GetAggregateScalarDataSeriesResponse.MY_QNAME,factory));
                                

                         return emptyEnvelope;
                    } catch(org.apache.axis2.databinding.ADBException e){
                        throw org.apache.axis2.AxisFault.makeFault(e);
                    }
                    }
                    
                         private org.tempuri.GetAggregateScalarDataSeriesResponse wrapGetAggregateScalarDataSeries(){
                                org.tempuri.GetAggregateScalarDataSeriesResponse wrappedElement = new org.tempuri.GetAggregateScalarDataSeriesResponse();
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
        
                if (org.tempuri.DeleteSensor.class.equals(type)){
                
                           return org.tempuri.DeleteSensor.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.DeleteSensorResponse.class.equals(type)){
                
                           return org.tempuri.DeleteSensorResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarDataSeriesInBatch.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarDataSeriesInBatch.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarDataSeriesInBatchResponse.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarDataSeriesInBatchResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreScalarDataBatch.class.equals(type)){
                
                           return org.tempuri.StoreScalarDataBatch.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreScalarDataBatchResponse.class.equals(type)){
                
                           return org.tempuri.StoreScalarDataBatchResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestVectorDataByComponentIndex.class.equals(type)){
                
                           return org.tempuri.GetLatestVectorDataByComponentIndex.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestVectorDataByComponentIndexResponse.class.equals(type)){
                
                           return org.tempuri.GetLatestVectorDataByComponentIndexResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestBinarySensorData.class.equals(type)){
                
                           return org.tempuri.GetLatestBinarySensorData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestBinarySensorDataResponse.class.equals(type)){
                
                           return org.tempuri.GetLatestBinarySensorDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.UpdateSensorLocation.class.equals(type)){
                
                           return org.tempuri.UpdateSensorLocation.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.UpdateSensorLocationResponse.class.equals(type)){
                
                           return org.tempuri.UpdateSensorLocationResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestScalarData.class.equals(type)){
                
                           return org.tempuri.GetLatestScalarData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestScalarDataResponse.class.equals(type)){
                
                           return org.tempuri.GetLatestScalarDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.RegisterSensor.class.equals(type)){
                
                           return org.tempuri.RegisterSensor.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.RegisterSensorResponse.class.equals(type)){
                
                           return org.tempuri.RegisterSensorResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarDataInBatch.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarDataInBatch.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarDataInBatchResponse.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarDataInBatchResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreVectorData.class.equals(type)){
                
                           return org.tempuri.StoreVectorData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreVectorDataResponse.class.equals(type)){
                
                           return org.tempuri.StoreVectorDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetSensorByPublisherAndName.class.equals(type)){
                
                           return org.tempuri.GetSensorByPublisherAndName.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetSensorByPublisherAndNameResponse.class.equals(type)){
                
                           return org.tempuri.GetSensorByPublisherAndNameResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestVectorData.class.equals(type)){
                
                           return org.tempuri.GetLatestVectorData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestVectorDataResponse.class.equals(type)){
                
                           return org.tempuri.GetLatestVectorDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetScalarDataSeriesInBatch.class.equals(type)){
                
                           return org.tempuri.GetScalarDataSeriesInBatch.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetScalarDataSeriesInBatchResponse.class.equals(type)){
                
                           return org.tempuri.GetScalarDataSeriesInBatchResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarData.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarDataResponse.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.SetSensorData.class.equals(type)){
                
                           return org.tempuri.SetSensorData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.SetSensorDataResponse.class.equals(type)){
                
                           return org.tempuri.SetSensorDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreBinaryData.class.equals(type)){
                
                           return org.tempuri.StoreBinaryData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreBinaryDataResponse.class.equals(type)){
                
                           return org.tempuri.StoreBinaryDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetScalarDataSeries.class.equals(type)){
                
                           return org.tempuri.GetScalarDataSeries.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetScalarDataSeriesResponse.class.equals(type)){
                
                           return org.tempuri.GetScalarDataSeriesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetSensorsByPublisher.class.equals(type)){
                
                           return org.tempuri.GetSensorsByPublisher.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetSensorsByPublisherResponse.class.equals(type)){
                
                           return org.tempuri.GetSensorsByPublisherResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestScalarDataInBatch.class.equals(type)){
                
                           return org.tempuri.GetLatestScalarDataInBatch.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetLatestScalarDataInBatchResponse.class.equals(type)){
                
                           return org.tempuri.GetLatestScalarDataInBatchResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.DataToString.class.equals(type)){
                
                           return org.tempuri.DataToString.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.DataToStringResponse.class.equals(type)){
                
                           return org.tempuri.DataToStringResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreVectorDataByComponentIndex.class.equals(type)){
                
                           return org.tempuri.StoreVectorDataByComponentIndex.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreVectorDataByComponentIndexResponse.class.equals(type)){
                
                           return org.tempuri.StoreVectorDataByComponentIndexResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.RegisterVectorSensor.class.equals(type)){
                
                           return org.tempuri.RegisterVectorSensor.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.RegisterVectorSensorResponse.class.equals(type)){
                
                           return org.tempuri.RegisterVectorSensorResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreScalarData.class.equals(type)){
                
                           return org.tempuri.StoreScalarData.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.StoreScalarDataResponse.class.equals(type)){
                
                           return org.tempuri.StoreScalarDataResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.DeleteVectorSensor.class.equals(type)){
                
                           return org.tempuri.DeleteVectorSensor.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.DeleteVectorSensorResponse.class.equals(type)){
                
                           return org.tempuri.DeleteVectorSensorResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarDataSeries.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarDataSeries.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

                }
           
                if (org.tempuri.GetAggregateScalarDataSeriesResponse.class.equals(type)){
                
                           return org.tempuri.GetAggregateScalarDataSeriesResponse.Factory.parse(param.getXMLStreamReaderWithoutCaching());
                    

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
    