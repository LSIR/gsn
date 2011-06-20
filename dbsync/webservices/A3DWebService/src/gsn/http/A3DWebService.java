package gsn.http;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.Style;

public interface A3DWebService {
    String[] getSensors();
    String[] getSensorInfo(String sensor);
    String[] getSensorLocation(String sensor);
    String[] getLatestMeteoData(String sensor);
    String[] getLatestMeteoDataMeasurement(String sensor, String measurement);
    String[] getMeteoData(String sensor, long from, long to);
    String[] getMeteoDataMeasurement(String sensor, String measurement, long from, long to);
}
