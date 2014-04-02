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
* File: webservices/A3DWebService/src/gsn/http/A3DWebService.java
*
* @author Sofiane Sarni
*
*/

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
