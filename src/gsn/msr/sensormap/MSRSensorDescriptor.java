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
* File: src/gsn/msr/sensormap/MSRSensorDescriptor.java
*
* @author Ali Salehi
*
*/

package gsn.msr.sensormap;

import java.util.Date;

public class MSRSensorDescriptor {
  private String publisherName, publisherID="", sensorName;
  
  private Double longitude, latitude, altitude;
  
  private int    reportPeriod, samplingPeriod;          // in seconds
  
 
  private String dataType;                              //
  
  private String description, keywords, url, unit;
  
  public static String DATA_TYPE_SCALAR = "scalar";
  public static String DATA_TYPE_HTML = "html";
  public static String DATA_TYPE_BMP = "bmp";
  public static String DATA_TYPE_JPG = "jpg";
  public static String DATA_TYPE_GIF = "gif";
  
  public static String SENSOR_TYPE_UNKNOWN =  "http://research.microsoft.com/nec/sensor/type/SensorType.owl#Unknown";
  public static String SENSOR_TYPE_THERMOMETER = "http://research.microsoft.com/nec/sensor/type/SensorType.owl#Thermometer";
  public static String SENSOR_TYPE_VIDEOCAMERA = "http://research.microsoft.com/nec/sensor/type/SensorType.owl#VideoCamera";
  public static String SENSOR_TYPE_WEATHER = "http://research.microsoft.com/nec/sensor/type/SensorType.owl#Weather";
  public static String SENSOR_TYPE_TRAFFIC = "http://research.microsoft.com/nec/sensor/type/SensorType.owl#Traffic";
  public static String SENSOR_TYPE_PARKING ="http://research.microsoft.com/nec/sensor/type/SensorType.owl#Parking";
  public static String SENSOR_TYPE_GENERIC =  "http://research.microsoft.com/nec/sensor/type/SensorType.owl#Generic";
  public static String SENSOR_TYPE_GEORSS =  "http://research.microsoft.com/nec/sensor/type/SensorType.owl#GeoRSS";
  
  private String sensorType;
 
  private Date   entryTime = null;

  public String getPublisherName() {
    return publisherName;
  }


  public void setPublisherName(String publisherName) {
    this.publisherName = publisherName;
  }


  public String getPublisherID() {
    return publisherID;
  }


  public void setPublisherID(String publisherID) {
    this.publisherID = publisherID;
  }


  public String getSensorName() {
    return sensorName;
  }


  public void setSensorName(String sensorName) {
    this.sensorName = sensorName;
  }


  public Double getLongitude() {
    return longitude;
  }


  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }


  public Double getLatitude() {
    return latitude;
  }


  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }


  public Double getAltitude() {
    return altitude;
  }


  public void setAltitude(Double altitude) {
    this.altitude = altitude;
  }


  public int getReportPeriod() {
    return reportPeriod;
  }


  public void setReportPeriod(int reportPeriod) {
    this.reportPeriod = reportPeriod;
  }


  public int getSamplingPeriod() {
    return samplingPeriod;
  }


  public void setSamplingPeriod(int samplingPeriod) {
    this.samplingPeriod = samplingPeriod;
  }


  public String getDataType() {
    return dataType;
  }


  public void setDataType(String dataType) {
    this.dataType = dataType;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }


  public String getKeywords() {
    return keywords;
  }


  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }


  public String getUrl() {
    return url;
  }


  public void setUrl(String url) {
    this.url = url;
  }


  public String getUnit() {
    return unit;
  }


  public void setUnit(String unit) {
    this.unit = unit;
  }


  public String getSensorType() {
    return sensorType;
  }


  public void setSensorType(String sensorType) {
    this.sensorType = sensorType;
  }


  public Date getEntryTime() {
    return entryTime;
  }


  public void setEntryTime(Date entryTime) {
    this.entryTime = entryTime;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((publisherName == null) ? 0 : publisherName.hashCode());
    result = prime * result
        + ((sensorName == null) ? 0 : sensorName.hashCode());
    result = prime * result
        + ((sensorType == null) ? 0 : sensorType.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final MSRSensorDescriptor other = (MSRSensorDescriptor) obj;
    if (publisherName == null) {
      if (other.publisherName != null)
        return false;
    } else if (!publisherName.equals(other.publisherName))
      return false;
    if (sensorName == null) {
      if (other.sensorName != null)
        return false;
    } else if (!sensorName.equals(other.sensorName))
      return false;
    if (sensorType == null) {
      if (other.sensorType != null)
        return false;
    } else if (!sensorType.equals(other.sensorType))
      return false;
    return true;
  }
  
  
  
}
