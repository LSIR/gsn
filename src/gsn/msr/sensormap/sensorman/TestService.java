/**
 * ServiceTest.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.3  Built on : Aug 10, 2007 (04:45:47 LKT)
 */
package gsn.msr.sensormap.sensorman;


/*
 *  TestService Junit test case
 */
public class TestService extends junit.framework.TestCase {
    /**
     * Auto generated test method
     */
    public void testCreateSensorType() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.CreateSensorType createSensorType30 =
            (gsn.msr.sensormap.sensorman.ServiceStub.CreateSensorType) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.CreateSensorType.class);
        // TODO : Fill in the createSensorType30 here
        assertNotNull(stub.CreateSensorType(createSensorType30));
    }

    /**
     * Auto generated test method
     */
    public void testCreateVectorSensorType() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorType createVectorSensorType32 =
            (gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorType) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorType.class);
        // TODO : Fill in the createVectorSensorType32 here
        assertNotNull(stub.CreateVectorSensorType(createVectorSensorType32));
    }

    /**
     * Auto generated test method
     */
    public void testCreateVectorSensorTypeByIds() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorTypeByIds createVectorSensorTypeByIds34 =
            (gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorTypeByIds) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.CreateVectorSensorTypeByIds.class);
        // TODO : Fill in the createVectorSensorTypeByIds34 here
        assertNotNull(stub.CreateVectorSensorTypeByIds(
                createVectorSensorTypeByIds34));
    }

    /**
     * Auto generated test method
     */
    public void testGetSensorTypeList() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.GetSensorTypeList getSensorTypeList36 =
            (gsn.msr.sensormap.sensorman.ServiceStub.GetSensorTypeList) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.GetSensorTypeList.class);
        // TODO : Fill in the getSensorTypeList36 here
        assertNotNull(stub.GetSensorTypeList(getSensorTypeList36));
    }

    /**
     * Auto generated test method
     */
    public void testGetVectorSensorTypeList() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.GetVectorSensorTypeList getVectorSensorTypeList38 =
            (gsn.msr.sensormap.sensorman.ServiceStub.GetVectorSensorTypeList) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.GetVectorSensorTypeList.class);
        // TODO : Fill in the getVectorSensorTypeList38 here
        assertNotNull(stub.GetVectorSensorTypeList(getVectorSensorTypeList38));
    }

    /**
     * Auto generated test method
     */
    public void testRegisterSensor() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.RegisterSensor registerSensor40 = (gsn.msr.sensormap.sensorman.ServiceStub.RegisterSensor) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.RegisterSensor.class);
        // TODO : Fill in the registerSensor40 here
        assertNotNull(stub.RegisterSensor(registerSensor40));
    }

    /**
     * Auto generated test method
     */
    public void testRegisterVectorSensor() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.RegisterVectorSensor registerVectorSensor42 =
            (gsn.msr.sensormap.sensorman.ServiceStub.RegisterVectorSensor) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.RegisterVectorSensor.class);
        // TODO : Fill in the registerVectorSensor42 here
        assertNotNull(stub.RegisterVectorSensor(registerVectorSensor42));
    }

    /**
     * Auto generated test method
     */
    public void testDeleteSensor() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.DeleteSensor deleteSensor44 = (gsn.msr.sensormap.sensorman.ServiceStub.DeleteSensor) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.DeleteSensor.class);
        // TODO : Fill in the deleteSensor44 here
        assertNotNull(stub.DeleteSensor(deleteSensor44));
    }

    /**
     * Auto generated test method
     */
    public void testDeleteVectorSensor() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor deleteVectorSensor46 =
            (gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.DeleteVectorSensor.class);
        // TODO : Fill in the deleteVectorSensor46 here
        assertNotNull(stub.DeleteVectorSensor(deleteVectorSensor46));
    }

    /**
     * Auto generated test method
     */
    public void testUpdateSensorLocation() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.UpdateSensorLocation updateSensorLocation48 =
            (gsn.msr.sensormap.sensorman.ServiceStub.UpdateSensorLocation) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.UpdateSensorLocation.class);
        // TODO : Fill in the updateSensorLocation48 here
        assertNotNull(stub.UpdateSensorLocation(updateSensorLocation48));
    }

    /**
     * Auto generated test method
     */
    public void testGetSensorByPublisherAndName() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.GetSensorByPublisherAndName getSensorByPublisherAndName50 =
            (gsn.msr.sensormap.sensorman.ServiceStub.GetSensorByPublisherAndName) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.GetSensorByPublisherAndName.class);
        // TODO : Fill in the getSensorByPublisherAndName50 here
        assertNotNull(stub.GetSensorByPublisherAndName(
                getSensorByPublisherAndName50));
    }

    /**
     * Auto generated test method
     */
    public void testGetSensorsByPublisher() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPublisher getSensorsByPublisher52 =
            (gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPublisher) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPublisher.class);
        // TODO : Fill in the getSensorsByPublisher52 here
        assertNotNull(stub.GetSensorsByPublisher(getSensorsByPublisher52));
    }

    /**
     * Auto generated test method
     */
    public void testGetSensorsByPolygonQuery() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPolygonQuery getSensorsByPolygonQuery54 =
            (gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPolygonQuery) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.GetSensorsByPolygonQuery.class);
        // TODO : Fill in the getSensorsByPolygonQuery54 here
        assertNotNull(stub.GetSensorsByPolygonQuery(getSensorsByPolygonQuery54));
    }

    /**
     * Auto generated test method
     */
    public void testDebugSensor() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.DebugSensor debugSensor56 = (gsn.msr.sensormap.sensorman.ServiceStub.DebugSensor) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.DebugSensor.class);
        // TODO : Fill in the debugSensor56 here
        assertNotNull(stub.DebugSensor(debugSensor56));
    }

    /**
     * Auto generated test method
     */
    public void testDebugVectorSensor() throws java.lang.Exception {
        gsn.msr.sensormap.sensorman.ServiceStub stub = new gsn.msr.sensormap.sensorman.ServiceStub(); //the default implementation should point to the right endpoint

        gsn.msr.sensormap.sensorman.ServiceStub.DebugVectorSensor debugVectorSensor58 =
            (gsn.msr.sensormap.sensorman.ServiceStub.DebugVectorSensor) getTestObject(gsn.msr.sensormap.sensorman.ServiceStub.DebugVectorSensor.class);
        // TODO : Fill in the debugVectorSensor58 here
        assertNotNull(stub.DebugVectorSensor(debugVectorSensor58));
    }

    //Create an ADBBean and provide it as the test object
    public org.apache.axis2.databinding.ADBBean getTestObject(
        java.lang.Class type) throws Exception {
        return (org.apache.axis2.databinding.ADBBean) type.newInstance();
    }
}
