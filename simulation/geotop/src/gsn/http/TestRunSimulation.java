package gsn.http;

import org.apache.log4j.PropertyConfigurator;

public class TestRunSimulation {

    private static RunSimulationWebService runSimulation = new RunSimulationWebServiceImpl();

    public static void main(String[] argv) {

                PropertyConfigurator.configure ( gsn.Main.DEFAULT_GSN_LOG4J_PROPERTIES );
        
        String client =    runSimulation.registerClient("","");
        System.out.println(client);
        System.out.println(runSimulation.setGeneralParameters(client,"2010-01-01T01:00:00.000+01:00","2010-01-01T01:00:00.000+01:00","station1,wan2","geotop",5,"",""));
        System.out.println(runSimulation.getSimulationParameters(client));
        System.out.println("Status: "+runSimulation.getSimulationStatus(client));
        System.out.println("Adding parameter: "+runSimulation.addSimulationParameter(client, "Filter","HNW::filter1 = accumulate , HNW::arg1 = 3600 " ));
        System.out.println("Trying to start simulation : "+runSimulation.startSimulation(client));
        System.out.println("Trying to stop simulation : "+runSimulation.stopSimulation(client));
    }
}
