package gsn.http;

import gsn.simulation.GEOtopSimulation;
import gsn.simulation.GEOtopSimulationParameters;
import gsn.utils.GSNRuntimeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RunSimulationWebServiceImpl implements RunSimulationWebService {

    private static List<String> listOfClients = new ArrayList<String>();
    private static HashMap<String, GEOtopSimulation> simulations = new HashMap<String, GEOtopSimulation>();

    public String registerClient(String user, String password) {
        //todo: check credentials

        //todo: check if already allocated

        String newUUID = generateUUID();

        listOfClients.add(newUUID);
        simulations.put(newUUID, new GEOtopSimulation(newUUID));

        return newUUID;
    }

    private GEOtopSimulation getSimulation(String UUID) {
        return simulations.get(UUID);
    }

    private boolean isClientRegistered(String UUID) {
        return simulations.containsKey(UUID);
    }


    public String setGeneralParameters(String uuid, String startfrom, String endto, String stations, String type, long windowsize, String scriptname, String scriptparams) {
        String result = "";
        if (!isClientRegistered(uuid)) {
            return "Error: client " + uuid + " is not registered.";
        } else {
            try {
                //setGeneralParameters(String startFrom, String endTo, String stations, String type, long windowsize, String scriptname, String scriptparams)  {
                //getSimulation(uuid).setGeneralParameters(startfrom, endto, stations, type, windowsize, scriptname, scriptparams);
            } catch (GSNRuntimeException e) {
                return "Error: " + e.getMessage();
            }
        }
        return "Ok";
    }

    public String setSimulationDEM(String uuid, String DEM) {
        return null;
    }

    public String addSimulationFilter(String uuid, String field, String filter, String parameters) {
        return null;
    }

    public String addSimulationParameter(String uuid, String type, String parameter) {
        if (!isClientRegistered(uuid)) {
            return "Error: client " + uuid + " is not registered.";
        } else {
            return getSimulation(uuid).addSimulationParameter(parameter);
        }
    }

    public String startSimulation(String uuid) {
        if (!isClientRegistered(uuid)) {
            return "Error: client " + uuid + " is not registered.";
        } else {
            int result = getSimulation(uuid).start();
            switch (result) {
                case 0 : return "Ok";
                case -1 : return "Cannot start simulation ("+result+")";
                case -2 : return "Simulation already running ("+result+")";
                default:  return "Unkonown error ("+result+")";
            }
        }
    }

    public String stopSimulation(String uuid) {
        if (!isClientRegistered(uuid)) {
            return "Error: client " + uuid + " is not registered.";
        } else {
            int result = getSimulation(uuid).stop();
            switch (result) {
                case 0 : return "Ok";
                case -1 : return "Cannot stop simulation ("+result+")";
                case -2 : return "Simulation already stopped ("+result+")";
                default:  return "Unkonown error ("+result+")";
            }
        }
    }

    public String getSimulationStatus(String uuid) {
        if (!isClientRegistered(uuid)) {
            return "Error: client " + uuid + " is not registered.";
        } else {
            return getSimulation(uuid).getSimulationStatus();
        }
    }

    public int getLatestSimulationStep(String uuid) {
        return 0;
    }

    public int getLatestSimulationStatus(String uuid) {
        return 0;
    }

    public String getSimulationResult(String uuid, int step) {
        return null;
    }

    public String getSimulationOutput(String uuid, int step, String file) {
        return null;
    }

    public String setFilesToFetch(String uuid, String fileNames) {
        return null;
    }

    public String getSimulationParameters(String uuid) {
        if (!isClientRegistered(uuid)) {
            return "Error: client " + uuid + " is not registered.";
        } else {
            return getSimulation(uuid).toString();
        }
    }

    private void cleanResultsDirectory() {

    }


    /*
   * Generates a random UUID
   * */
    private String generateUUID() {
        return generateUUID(false);
    }

    /*
   * Generates a random UUID
   * if flag is set to true, uses Java UUID
   * */
    private String generateUUID(boolean useJavaUUID) {
        if (useJavaUUID) {
            return UUID.randomUUID().toString();
        } else {
            byte oneCharacter;
            StringBuffer result = new StringBuffer(8);
            for (int i = 0; i < 8; i++) {
                oneCharacter = (byte) ((Math.random() * ('z' - 'a' + 1)) + 'a');
                result.append((char) oneCharacter);
            }
            return result.toString();
        }
    }
}
