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
* File: src/gsn/webservice/standard/GSNWebServiceCallbackHandler.java
*
* @author Sofiane Sarni
*
*/

/**
 * GSNWebServiceCallbackHandler.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.5.3  Built on : Nov 12, 2010 (02:24:07 CET)
 */

package gsn.webservice.standard;

/**
 * GSNWebServiceCallbackHandler Callback class, Users can extend this class and implement
 * their own receiveResult and receiveError methods.
 */
public abstract class GSNWebServiceCallbackHandler {


    protected Object clientData;

    /**
     * User can pass in any object that needs to be accessed once the NonBlocking
     * Web service call is finished and appropriate method of this CallBack is called.
     *
     * @param clientData Object mechanism by which the user can pass in user data
     *                   that will be avilable at the time this callback is called.
     */
    public GSNWebServiceCallbackHandler(Object clientData) {
        this.clientData = clientData;
    }

    /**
     * Please use this constructor if you don't want to set any clientData
     */
    public GSNWebServiceCallbackHandler() {
        this.clientData = null;
    }

    /**
     * Get the client data
     */

    public Object getClientData() {
        return clientData;
    }


    /**
     * auto generated Axis2 call back method for getVirtualSensorsDetails method
     * override this method for handling normal response from getVirtualSensorsDetails operation
     */
    public void receiveResultgetVirtualSensorsDetails(
            gsn.webservice.standard.GSNWebServiceStub.GetVirtualSensorsDetailsResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getVirtualSensorsDetails operation
     */
    public void receiveErrorgetVirtualSensorsDetails(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getNextData method
     * override this method for handling normal response from getNextData operation
     */
    public void receiveResultgetNextData(
            gsn.webservice.standard.GSNWebServiceStub.GetNextDataResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getNextData operation
     */
    public void receiveErrorgetNextData(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for listWrapperURLs method
     * override this method for handling normal response from listWrapperURLs operation
     */
    public void receiveResultlistWrapperURLs(
            gsn.webservice.standard.GSNWebServiceStub.ListWrapperURLsResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from listWrapperURLs operation
     */
    public void receiveErrorlistWrapperURLs(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getLatestMultiData method
     * override this method for handling normal response from getLatestMultiData operation
     */
    public void receiveResultgetLatestMultiData(
            gsn.webservice.standard.GSNWebServiceStub.GetLatestMultiDataResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getLatestMultiData operation
     */
    public void receiveErrorgetLatestMultiData(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for unregisterQuery method
     * override this method for handling normal response from unregisterQuery operation
     */
    public void receiveResultunregisterQuery(
            gsn.webservice.standard.GSNWebServiceStub.UnregisterQueryResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from unregisterQuery operation
     */
    public void receiveErrorunregisterQuery(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for createVirtualSensor method
     * override this method for handling normal response from createVirtualSensor operation
     */
    public void receiveResultcreateVirtualSensor(
            gsn.webservice.standard.GSNWebServiceStub.CreateVirtualSensorResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from createVirtualSensor operation
     */
    public void receiveErrorcreateVirtualSensor(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getMultiData method
     * override this method for handling normal response from getMultiData operation
     */
    public void receiveResultgetMultiData(
            gsn.webservice.standard.GSNWebServiceStub.GetMultiDataResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getMultiData operation
     */
    public void receiveErrorgetMultiData(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for registerQuery method
     * override this method for handling normal response from registerQuery operation
     */
    public void receiveResultregisterQuery(
            gsn.webservice.standard.GSNWebServiceStub.RegisterQueryResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from registerQuery operation
     */
    public void receiveErrorregisterQuery(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for getContainerInfo method
     * override this method for handling normal response from getContainerInfo operation
     */
    public void receiveResultgetContainerInfo(
            gsn.webservice.standard.GSNWebServiceStub.GetContainerInfoResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from getContainerInfo operation
     */
    public void receiveErrorgetContainerInfo(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for listVirtualSensorNames method
     * override this method for handling normal response from listVirtualSensorNames operation
     */
    public void receiveResultlistVirtualSensorNames(
            gsn.webservice.standard.GSNWebServiceStub.ListVirtualSensorNamesResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from listVirtualSensorNames operation
     */
    public void receiveErrorlistVirtualSensorNames(java.lang.Exception e) {
    }

    /**
     * auto generated Axis2 call back method for deleteVirtualSensor method
     * override this method for handling normal response from deleteVirtualSensor operation
     */
    public void receiveResultdeleteVirtualSensor(
            gsn.webservice.standard.GSNWebServiceStub.DeleteVirtualSensorResponse result
    ) {
    }

    /**
     * auto generated Axis2 Error handler
     * override this method for handling error response from deleteVirtualSensor operation
     */
    public void receiveErrordeleteVirtualSensor(java.lang.Exception e) {
    }


}
    