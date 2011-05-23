package gsn.http;

public interface RunSimulationWebService {
    String registerClient(String user, String password);
    String setGeneralParameters(String uuid, String startfrom, String endto, String stations, String type, long windowsize, String scriptname, String scriptparams);
    String setSimulationDEM(String uuid, String DEM);
    String addSimulationFilter(String uuid, String field, String filter, String parameters);
    String addSimulationParameter(String uuid, String type, String parameter);
    String startSimulation(String uuid);
    String stopSimulation(String uuid);
    String getSimulationStatus(String uuid);
    int getLatestSimulationStep(String uuid);
    int getLatestSimulationStatus(String uuid);
    String getSimulationResult(String uuid, int step);
    String getSimulationOutput(String uuid, int step, String fileName);
    String setFilesToFetch(String uuid, String fileNames);
    String getSimulationParameters(String uuid);
}
