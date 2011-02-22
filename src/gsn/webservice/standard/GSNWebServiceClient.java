package gsn.webservice.standard;

import org.apache.axis2.AxisFault;

import java.rmi.RemoteException;

public class GSNWebServiceClient {


    public static String[] listVirtualSensorNames(String EPR) {

        String[] sensors = null;

        try {
            GSNWebServiceStub stub = new GSNWebServiceStub(EPR);
            GSNWebServiceStub.ListVirtualSensorNames request = new GSNWebServiceStub.ListVirtualSensorNames();
            GSNWebServiceStub.ListVirtualSensorNamesResponse response = stub.listVirtualSensorNames(request);
            sensors = response.getVirtualSensorName();
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return sensors;
    }

    public static void getMultiData(String EPR, String vsName, long from, long to, int nb) {
        try {
            GSNWebServiceStub stub = new GSNWebServiceStub(EPR);
            GSNWebServiceStub.GetMultiData request = new GSNWebServiceStub.GetMultiData();
            GSNWebServiceStub.GSNWebService_FieldSelector virtualSensors = new GSNWebServiceStub.GSNWebService_FieldSelector();
            virtualSensors.setVsname(vsName);
            request.addFieldSelector(virtualSensors);
            request.setFrom(from);
            request.setTo(to);
            request.setNb(nb);
            System.out.println(request.getFieldSelector()[0].getVsname());

            GSNWebServiceStub.GetMultiDataResponse response = stub.getMultiData(request);
            if (response.getQueryResult() != null) {
                GSNWebServiceStub.GSNWebService_QueryResult[] query_result = response.getQueryResult();
                int query_result_length = query_result.length;
                System.out.println("Result length: " + query_result_length);

                for (int i = 0; i < query_result_length; i++) {
                    String executed_query = query_result[i].getExecutedQuery();
                    String vs_name = query_result[i].getVsname();
                    System.out.println(i + " : " + vs_name);
                    System.out.println("   " + executed_query);

                    if (query_result[i].getFormat().getField() != null) {
                        int fields_length = query_result[i].getFormat().getField().length;
                        System.out.println("fields: " + fields_length);
                        for (int k = 0; k < fields_length; k++) {
                            String field_name = query_result[i].getFormat().getField()[k].getName();
                            String field_description = query_result[i].getFormat().getField()[k].getDescription();
                            String field_type = query_result[i].getFormat().getField()[k].getType();
                            System.out.println("   " + field_name + "(" + field_type + ") : " + field_description);
                        }
                    }

                    if (query_result[i].getStreamElements() != null) {
                        int stream_elements_length = query_result[i].getStreamElements().length;
                        for (int j = 0; j < stream_elements_length; j++) {
                            String timed = query_result[i].getStreamElements()[j].getTimed();
                            System.out.println("timed: " + timed);
                            if (query_result[i].getStreamElements()[j].getField() != null) {
                                int fields_length = query_result[i].getStreamElements()[j].getField().length;
                                for (int k = 0; k < fields_length; k++) {
                                    String field_string = query_result[i].getStreamElements()[j].getField()[k].getString();
                                    System.out.println(field_string);
                                }

                            }

                        }
                    }

                }
            }

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public static void getLatestMultiData(String EPR, String vsName) {
        try {
            GSNWebServiceStub stub = new GSNWebServiceStub(EPR);
            GSNWebServiceStub.GetLatestMultiData request = new GSNWebServiceStub.GetLatestMultiData();
            GSNWebServiceStub.GSNWebService_FieldSelector virtualSensors = new GSNWebServiceStub.GSNWebService_FieldSelector();
            virtualSensors.setVsname(vsName);
            request.addFieldSelector(virtualSensors);
            System.out.println(request.getFieldSelector()[0].getVsname());

            GSNWebServiceStub.GetLatestMultiDataResponse response = stub.getLatestMultiData(request);
            if (response.getQueryResult() != null) {
                GSNWebServiceStub.GSNWebService_QueryResult[] query_result = response.getQueryResult();

                int query_result_length = query_result.length;
                System.out.println("Result length: " + query_result_length);
                for (int i = 0; i < query_result_length; i++) {
                    String executed_query = query_result[i].getExecutedQuery();
                    String vs_name = query_result[i].getVsname();
                    System.out.println(i + " : " + vs_name);
                    System.out.println("   " + executed_query);
                    if (query_result[i].getFormat().getField() != null) {
                        int fields_length = query_result[i].getFormat().getField().length;
                        System.out.println("fields: " + fields_length);
                        for (int k = 0; k < fields_length; k++) {
                            String field_name = query_result[i].getFormat().getField()[k].getName();
                            String field_description = query_result[i].getFormat().getField()[k].getDescription();
                            String field_type = query_result[i].getFormat().getField()[k].getType();
                            System.out.println("   " + field_name + "(" + field_type + ") : " + field_description);
                        }
                    }

                    if (query_result[i].getStreamElements() != null) {
                        int stream_elements_length = query_result[i].getStreamElements().length;
                        for (int j = 0; j < stream_elements_length; j++) {
                            String timed = query_result[i].getStreamElements()[j].getTimed();
                            System.out.println("timed: " + timed);
                            if (query_result[i].getStreamElements()[j].getField() != null) {
                                int fields_length = query_result[i].getStreamElements()[j].getField().length;
                                for (int k = 0; k < fields_length; k++) {
                                    String field_string = query_result[i].getStreamElements()[j].getField()[k].getString();
                                    System.out.println(field_string);
                                }

                            }

                        }
                    }
                }
            }

        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void getVirtualSensorAddressing(String EPR, String vsName) {

        try {
            GSNWebServiceStub stub = new GSNWebServiceStub(EPR);
            GSNWebServiceStub.GetVirtualSensorsDetails request = new GSNWebServiceStub.GetVirtualSensorsDetails();

            GSNWebServiceStub.GSNWebService_FieldSelector virtualSensors = new GSNWebServiceStub.GSNWebService_FieldSelector();
            virtualSensors.setVsname(vsName);
            request.addFieldSelector(virtualSensors);
            System.out.println(request.getFieldSelector()[0].getVsname());

            GSNWebServiceStub.GSNWebService_DetailsType detailsType = new GSNWebServiceStub.GSNWebService_DetailsType(GSNWebServiceStub.GSNWebService_DetailsType._ADDRESSING, true);
            System.out.println(detailsType.getValue());
            request.addDetailsType(detailsType);

            GSNWebServiceStub.GetVirtualSensorsDetailsResponse response = stub.getVirtualSensorsDetails(request);

            GSNWebServiceStub.GSNWebService_VirtualSensorDetails[] virtualSensorDetails = response.getVirtualSensorDetails();

            if (virtualSensorDetails != null) {
                System.out.println(virtualSensorDetails.length);

                for (int i = 0; i < response.getVirtualSensorDetails().length; i++) {
                    String vs_name = response.getVirtualSensorDetails()[i].getVsname();
                    int predicates_length = 0;
                    if (response.getVirtualSensorDetails()[i].getAddressing().getPredicates() != null)
                        predicates_length = response.getVirtualSensorDetails()[i].getAddressing().getPredicates().length;
                    System.out.println(vs_name + " : " + predicates_length);
                    for (int j = 0; j < predicates_length; j++) {
                        String predicate_name = response.getVirtualSensorDetails()[i].getAddressing().getPredicates()[j].getName();
                        String predicate_value = response.getVirtualSensorDetails()[i].getAddressing().getPredicates()[j].getString();
                        System.out.println("   - " + predicate_name + " : " + predicate_value);
                    }
                }
            }
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void getVirtualSensorStructure(String EPR, String vsName) {

        try {
            GSNWebServiceStub stub = new GSNWebServiceStub(EPR);
            GSNWebServiceStub.GetVirtualSensorsDetails request = new GSNWebServiceStub.GetVirtualSensorsDetails();

            GSNWebServiceStub.GSNWebService_FieldSelector virtualSensors = new GSNWebServiceStub.GSNWebService_FieldSelector();
            virtualSensors.setVsname(vsName);
            request.addFieldSelector(virtualSensors);
            System.out.println(request.getFieldSelector()[0].getVsname());

            GSNWebServiceStub.GSNWebService_DetailsType detailsType = new GSNWebServiceStub.GSNWebService_DetailsType(GSNWebServiceStub.GSNWebService_DetailsType._OUTPUTSTRUCTURE, true);
            System.out.println(detailsType.getValue());
            request.addDetailsType(detailsType);

            GSNWebServiceStub.GetVirtualSensorsDetailsResponse response = stub.getVirtualSensorsDetails(request);

            GSNWebServiceStub.GSNWebService_VirtualSensorDetails[] virtualSensorDetails = response.getVirtualSensorDetails();

            if (virtualSensorDetails != null) {
                System.out.println(virtualSensorDetails.length);

                for (int i = 0; i < response.getVirtualSensorDetails().length; i++) {
                    String vs_name = response.getVirtualSensorDetails()[i].getVsname();
                    int predicates_length = 0;
                    int output_structure_length = 0;
                    if (response.getVirtualSensorDetails()[i].getOutputStructure() != null && response.getVirtualSensorDetails()[i].getOutputStructure().getFields() != null)
                        output_structure_length = response.getVirtualSensorDetails()[i].getOutputStructure().getFields().length;

                    System.out.println(vs_name + " : " + output_structure_length);
                    for (int j = 0; j < output_structure_length; j++) {
                        String field_name = response.getVirtualSensorDetails()[i].getOutputStructure().getFields()[j].getName();
                        String field_type = response.getVirtualSensorDetails()[i].getOutputStructure().getFields()[j].getType();
                        System.out.println("   "+field_name + " : " + field_type);
                    }
                }
            }
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Usage: GSNWebServiceClient <Web service endpoint>");
            System.err.println("e.g. GSNWebServiceClient http://localhost:22001/services/GSNWebService");
            System.exit(1);
        }

        System.out.println(args[0]);

        String EPR = args[0];

        String sensors[] = listVirtualSensorNames(EPR);

        System.out.println("Sensors at : " + EPR);
        if (sensors != null)
            for (int i = 0; i < sensors.length; i++) {
                System.out.println(i + " " + sensors[i]);
            }
        else
            System.out.println("No sensors found.");

        getVirtualSensorStructure(EPR, "ALL");

        getVirtualSensorAddressing(EPR, "ALL");

        getLatestMultiData(EPR, "ALL");

        long from = 0;
        long to = 12811638922851L;
        int nb = 5;
        getMultiData(EPR, "ALL", from, to, nb);

    }


}
