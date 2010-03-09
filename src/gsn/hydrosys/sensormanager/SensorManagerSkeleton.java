/**
 * SensorManagerSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:35 LKT)
 */
package gsn.hydrosys.sensormanager;

import gsn.Mappings;
import gsn.beans.VSensorConfig;
import gsn.hydrosys.sensormanager.xsd.Status;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;


/**
 * SensorManagerSkeleton java skeleton for the axisService
 */
public class SensorManagerSkeleton {

    private static final transient org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SensorManagerSkeleton.class);

    private static final byte STATUS_OK = 0x01;
    private static final byte STATUS_ERR = 0x02;


    /**
     * Auto generated method signature
     *
     * @param createVirtualSensor
     */
    public gsn.hydrosys.sensormanager.CreateVirtualSensorResponse createVirtualSensor(gsn.hydrosys.sensormanager.CreateVirtualSensor createVirtualSensor) {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#createVirtualSensor");
        CreateVirtualSensorResponse response = new CreateVirtualSensorResponse();
        String message = "Created the configuration file (" + gsn.VSensorLoader.getVSConfigurationFilePath(createVirtualSensor.getVsname()) + ")";
        Status status = new Status();
        status.setStatus(STATUS_OK);
        //
        try {
            gsn.VSensorLoader.getInstance(gsn.Main.DEFAULT_VIRTUAL_SENSOR_DIRECTORY).loadVirtualSensor(
                    createVirtualSensor.getDescriptionFileContent(),
                    createVirtualSensor.getVsname()
            );
        } catch (Exception e) {
            message = "Unable to create the configuration file (" + gsn.VSensorLoader.getVSConfigurationFilePath(createVirtualSensor.getVsname()) + ")\nCause " + e.getMessage();
            status.setStatus(STATUS_ERR);
        }
        //
        status.setMessage(message);
        response.setCreateVirtualSensorResponse(status);
        logger.info(status.getMessage());
        return response;
    }

    /**
     * Auto generated method signature
     *
     * @param registerQuery
     */
    public gsn.hydrosys.sensormanager.RegisterQueryResponse registerQuery(gsn.hydrosys.sensormanager.RegisterQuery registerQuery) {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#registerQuery");
        CreateVirtualSensor creationDetails = new CreateVirtualSensor();
        String content = createVSConfigurationFileContent(registerQuery);
        creationDetails.setVsname(registerQuery.getQueryName());
        creationDetails.setDescriptionFileContent(
                content
        );
        CreateVirtualSensorResponse creationStatus = createVirtualSensor(creationDetails);
        Status status = creationStatus.getCreateVirtualSensorResponse();
        //
        RegisterQueryResponse response = new RegisterQueryResponse();
        status.setMessage(new StringBuilder().append("Query: ").append(registerQuery.getQuery()).append("\n").append(status.getMessage()).toString());
        response.setRegisterQueryResponse(status);
        logger.info(status.getMessage());
        return response;
    }


    /**
     * Auto generated method signature
     *
     * @param deleteVirtualSensor
     */

    public gsn.hydrosys.sensormanager.DeleteVirtualSensorResponse deleteVirtualSensor(gsn.hydrosys.sensormanager.DeleteVirtualSensor deleteVirtualSensor) {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#deleteVirtualSensor");
        DeleteVirtualSensorResponse response = new DeleteVirtualSensorResponse();
        Status status = new Status();
        if (unloadVirtualSensor(deleteVirtualSensor.getVsname())) {
            status.setMessage("Failed to delete the following Virtual Sensor: " + deleteVirtualSensor.getVsname());
            status.setStatus(STATUS_ERR);
        } else {
            status.setMessage("Deleted the following Virtual Sensor: " + deleteVirtualSensor.getVsname());
            status.setStatus(STATUS_OK);
        }
        response.setDeleteVirtualSensorResponse(status);
        logger.info(status.getMessage());
        return response;
    }


    /**
     * Auto generated method signature
     *
     * @param unregisterQuery
     */

    public gsn.hydrosys.sensormanager.UnregisterQueryResponse unregisterQuery(gsn.hydrosys.sensormanager.UnregisterQuery unregisterQuery) {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#unregisterQuery");
        UnregisterQueryResponse response = new UnregisterQueryResponse();
        Status status = new Status();
        if (unloadVirtualSensor(unregisterQuery.getQueryName())) {
            status.setMessage("Failed to unregister the following Query: " + unregisterQuery.getQueryName());
            status.setStatus(STATUS_ERR);
        } else {
            status.setMessage("Unregistered the following Query: " + unregisterQuery.getQueryName());
            status.setStatus(STATUS_OK);
        }
        response.setUnregisterQueryResponse(status);
        logger.info(status.getMessage());
        return response;
    }


    /**
     * Auto generated method signature
     */

    public gsn.hydrosys.sensormanager.ListAvailableWrappersResponse listAvailableWrappers() {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#listAvailableWrappers");
        ListAvailableWrappersResponse response = new ListAvailableWrappersResponse();
        ArrayList<String> wrappernames = new ArrayList<String>();
        for (Map.Entry property : gsn.Main.getInstance().getWrappers().entrySet()) {
            logger.debug("key: " + property.getKey() + " value: " + property.getValue());
            wrappernames.add(property.getKey().toString());
        }
        response.setWrappername(wrappernames.toArray(new String[wrappernames.size()]));
        return response;
    }


    /**
     * Auto generated method signature
     */

    public gsn.hydrosys.sensormanager.ListEnabledWrappersResponse listEnabledWrappers() {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#listEnabledWrappers");
        ListEnabledWrappersResponse response = new ListEnabledWrappersResponse();
        Iterator<VSensorConfig> iter = Mappings.getAllVSensorConfigs();
        VSensorConfig config;
        ArrayList<String> wrappers = new ArrayList<String>();
        while (iter.hasNext()) {
            config = iter.next();
            for (gsn.beans.InputStream is : config.getInputStreams()) {
                for (gsn.beans.StreamSource source : is.getSources()) {
                    if (!wrappers.contains(source.getActiveAddressBean().getWrapper()))
                        wrappers.add(source.getActiveAddressBean().getWrapper());
                }
            }
        }
        response.setWrappername(wrappers.toArray(new String[wrappers.size()]));
        return response;
    }


    /**
     * Auto generated method signature
     */

    public gsn.hydrosys.sensormanager.ListVirtualSensorsResponse listVirtualSensors() {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#listVirtualSensors");
        ListVirtualSensorsResponse response = new ListVirtualSensorsResponse();
        ArrayList<String> vsnames = new ArrayList<String>();
        Iterator<VSensorConfig> iter = Mappings.getAllVSensorConfigs();
        VSensorConfig config;
        while (iter.hasNext()) {
            config = iter.next();
            vsnames.add(config.getName());
        }
        response.setVsname(vsnames.toArray(new String[vsnames.size()]));
        return response;
    }


    /**
     * Auto generated method signature
     *
     * @param getOutputStructure
     */

    public gsn.hydrosys.sensormanager.GetOutputStructureResponse getOutputStructure(gsn.hydrosys.sensormanager.GetOutputStructure getOutputStructure) {
        //throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#getOutputStructure");
        GetOutputStructureResponse response = new GetOutputStructureResponse();
        VSensorConfig config = Mappings.getConfig(getOutputStructure.getVsname());
        if (config != null) {
            gsn.beans.DataField[] fields = config.getOutputStructure();
            for (int i = 0 ; i < fields.length ; i++) {
                gsn.hydrosys.sensormanager.xsd.DataField field = new gsn.hydrosys.sensormanager.xsd.DataField();
                field.setName(fields[i].getName());
                field.setType(fields[i].getType());
                response.addOutputstructure(field);
            }
        }
        return response;
    }


    // private methods


    private boolean unloadVirtualSensor(String virtualSensorName) {
        File vsConfigurationFile = new File(gsn.VSensorLoader.getVSConfigurationFilePath(virtualSensorName));
        return !vsConfigurationFile.delete();
    }

    private String createVSConfigurationFileContent(gsn.hydrosys.sensormanager.RegisterQuery registerQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("<virtual-sensor name=\"" + registerQuery.getQueryName() + "\" priority=\"10\" >\n");
        sb.append("             <processing-class>\n");
        sb.append("                     <class-name>gsn.vsensor.BridgeVirtualSensor</class-name>\n");
        sb.append("                     <init-params/>\n");
        sb.append("                     <output-structure>\n");
        gsn.hydrosys.sensormanager.xsd.DataField df;
        for (int i = 0; i < registerQuery.getOutputStructure().length; i++) {
            df = registerQuery.getOutputStructure()[i];
            sb.append("                     <field name=\"" + df.getName() + "\" type=\"" + df.getType() + "\"/>\n");
        }
        sb.append("                     </output-structure>\n");
        sb.append("             </processing-class>\n");
        sb.append("             <description>this VS implements the registered query: memquery</description>\n");
        sb.append("             <addressing>\n");
        sb.append("             </addressing>\n");
        sb.append("             <storage />\n");
        sb.append("             <streams>\n");
        sb.append("                     <stream name=\"data\">\n");
        String vsname;
        for (int i = 0; i < registerQuery.getVsnames().length; i++) {
            vsname = registerQuery.getVsnames()[i];
            sb.append("                     <source alias=\"" + vsname + "\" storage-size=\"1\" sampling-rate=\"1\">\n");
            sb.append("                             <address wrapper=\"local\">\n");
            sb.append("                                     <predicate key=\"NAME\">" + vsname + "</predicate>\n");
            sb.append("                             </address>\n");
            sb.append("                             <query>select * from wrapper</query>\n");
            sb.append("                     </source>\n");
        }
        sb.append("                             <query>" + registerQuery.getQuery() + "</query>\n");
        sb.append("                     </stream>\n");
        sb.append("             </streams>\n");
        sb.append("</virtual-sensor>");

        return sb.toString();
    }

}
