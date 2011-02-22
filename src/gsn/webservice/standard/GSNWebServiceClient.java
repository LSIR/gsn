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


    public static void getLatestMultiData(String EPR, String vsName) {

    }

    public static void getVirtualSensorDetails(String EPR, String vsName) {

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

        getVirtualSensorDetails(EPR, "ALL");

        getLatestMultiData(EPR, "ALL");

    }
}
