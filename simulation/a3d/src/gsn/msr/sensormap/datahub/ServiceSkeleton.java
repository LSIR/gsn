
/**
 * ServiceSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis2 version: 1.4.1  Built on : Aug 13, 2008 (05:03:35 LKT)
 */
    package gsn.msr.sensormap.datahub;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.DataEnumerator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.tempuri.ArrayOfDateTime;
import org.tempuri.ArrayOfDouble;
import org.tempuri.ArrayOfSensorData;
import org.tempuri.GetAggregateScalarDataInBatchResponse;
import org.tempuri.GetAggregateScalarDataResponse;
import org.tempuri.GetAggregateScalarDataSeriesInBatchResponse;
import org.tempuri.GetLatestScalarDataInBatchResponse;
import org.tempuri.GetScalarDataSeriesInBatchResponse;
import org.tempuri.SensorData;
    /**
     *  ServiceSkeleton java skeleton for the axisService
     */
    public class ServiceSkeleton{


private static final transient Logger         logger          = Logger.getLogger( ServiceSkeleton.class );
	/**
	 * SensorTypes :
	 * public  int Unknown = 0;
	 * public  int Generic = 1;
	 * public  int Temperature = 2;
	 * public  int Video = 3;
	 * public  int Traffic = 4;
	 * public  int Parking = 5;
	 * public  int Pressure = 6;
	 * public  int Humidity = 7;
	 * **************************
	 * DataTypes :
	 * public  int Unknown = 0;
	 * public  int Scalar = 1;
	 * public  int BMP = 2;
	 * public  int JPG = 3;
	 * public  int GIF = 4;
	 * public  int Vector = 5;
	 * public  int HTML = 6;
	 */


	public org.tempuri.GetAggregateScalarDataSeriesInBatchResponse GetAggregateScalarDataSeriesInBatch(org.tempuri.GetAggregateScalarDataSeriesInBatch input) {
		GetAggregateScalarDataSeriesInBatchResponse toReturn = new GetAggregateScalarDataSeriesInBatchResponse();

		long aggInMSec = input.getAggregateIntervalInSeconds()*1000;
		ArrayOfSensorData items = new ArrayOfSensorData () ;
		for (String signalInfo: input.getSensorNames().getString()) {
			try {
				SignalRequest req = new SignalRequest(signalInfo);
				StringBuilder query = new StringBuilder("select AVG(TIMED) as TIMED,AVG(").append(req.getFieldName()).append(") as data from ").append(req.getVsName()).append(" where TIMED >= ").append(input.getStartTime().getTimeInMillis()).append(" AND TIMED <= ").append(input.getEndTime().getTimeInMillis()).append(" group by FLOOR(TIMED/").append(aggInMSec).append(") order by TIMED");
				items.addSensorData(transformToSensorDataArray(req.getVsName(), query).getSensorData()[0] );
			}
			catch (RuntimeException e) {
				logger.debug("VS " + signalInfo + " not found");
				items.addSensorData(null);
			}
		}
		toReturn.setGetAggregateScalarDataSeriesInBatchResult(items);
		return toReturn;
	}

    public org.tempuri.GetAggregateScalarDataSeriesResponse GetAggregateScalarDataSeries(org.tempuri.GetAggregateScalarDataSeries input){
    	org.tempuri.GetAggregateScalarDataSeriesResponse toReturn = new org.tempuri.GetAggregateScalarDataSeriesResponse();

		long aggInMSec = input.getAggregateIntervalInSeconds()*1000;
		SensorData items = new SensorData () ;
		String signalInfo =input.getSensorName();

			try {
				SignalRequest req = new SignalRequest(signalInfo);
				StringBuilder query = new StringBuilder("select AVG(TIMED) as TIMED,AVG(").append(req.getFieldName()).append(") as data from ").append(req.getVsName()).append(" where TIMED >= ").append(input.getStartTime().getTimeInMillis()).append(" AND TIMED <= ").append(input.getEndTime().getTimeInMillis()).append(" group by FLOOR(TIMED/").append(aggInMSec).append(") order by TIMED");

                items.setData(transformToSensorDataArray(req.getVsName(), query).getSensorData()[0].getData() );

			}
			catch (RuntimeException e) {
				logger.debug("VS " + signalInfo + " not found");
				items.setData(null);
			}
		toReturn.setGetAggregateScalarDataSeriesResult(items);
		return toReturn;

    }

    public org.tempuri.GetAggregateScalarDataResponse GetAggregateScalarData(org.tempuri.GetAggregateScalarData input){
    	GetAggregateScalarDataResponse toReturn = new GetAggregateScalarDataResponse();

//		long aggInMSec = input.getAggregateIntervalInSeconds()*1000;
//		SensorData items = new SensorData () ;
//		String signalInfo =input.getSensorName();
//
//			try {
//				SignalRequest req = new SignalRequest(signalInfo);
//				StringBuilder query = new StringBuilder("select AVG(TIMED) as TIMED,AVG(").append(req.getFieldName()).append(") as data from ").append(req.getVsName()).append(" where TIMED >= ").append(input.getStartTime().getTimeInMillis()).append(" AND TIMED <= ").append(input.getEndTime().getTimeInMillis()).append(" group by FLOOR(TIMED/").append(aggInMSec).append(") order by TIMED");
//				items.setData(transformToSensorDataArray(query).getSensorData() );
//			}
//			catch (RuntimeException e) {
//				logger.debug("VS " + signalInfo + " not found");
//				items.setData(null);
//			}
//		toReturn.setGetAggregateScalarDataResult(items);
		return toReturn;

    }


	public org.tempuri.GetLatestScalarDataInBatchResponse GetLatestScalarDataInBatch(org.tempuri.GetLatestScalarDataInBatch input) {
		org.tempuri.GetLatestScalarDataInBatchResponse toReturn = new GetLatestScalarDataInBatchResponse();
		ArrayOfSensorData items = new ArrayOfSensorData();
		for (String signalInfo: input.getSensorNames().getString()) {
			try {
				SignalRequest req = new SignalRequest(signalInfo);
                StringBuilder query = new StringBuilder("select pk,TIMED, ").append(req.getFieldName()).append(" as data from ").append(req.getVsName());
                //if oracle
                if (Main.getStorage(req.getVsName()).isOracle()) {
                    query.append(" where rownum<=1 order by timed desc");
                }
                else {
				    query.append(" order by timed desc limit 0,1");
                }
				//			logger.fatal(query);
				items.addSensorData(transformToSensorDataArray(req.getVsName(), query).getSensorData()[0]);
			}
			catch (RuntimeException e) {
				logger.debug("VS " + signalInfo + " not found");
				items.addSensorData(null);
			}
		}
		toReturn.setGetLatestScalarDataInBatchResult(items);
		return toReturn;
	}

	/**
	 * Gets the data published by a set of sensor within a specified time window
	 * @param input
	 * @return
	 */
    public org.tempuri.GetScalarDataSeriesInBatchResponse GetScalarDataSeriesInBatch(org.tempuri.GetScalarDataSeriesInBatch input) {
    	//throw new java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetScalarDataSeriesInBatch");
        org.tempuri.GetScalarDataSeriesInBatchResponse toReturn = new GetScalarDataSeriesInBatchResponse () ;
        ArrayOfSensorData items = new ArrayOfSensorData();
        for (String signalInfo : input.getSensorNames().getString()) {
        	try {
        		SignalRequest req = new SignalRequest(signalInfo);
        		StringBuilder query = new StringBuilder("select TIMED, ").append(req.getFieldName()).append(" from ").append(req.getVsName()).append(" where TIMED >= ").append(input.getStartTime().getTimeInMillis()).append(" AND TIMED <= ").append(input.getEndTime().getTimeInMillis());
        		items.addSensorData(transformToSensorDataArray(req.getVsName(), query, false).getSensorData()[0]);
        	}
        	catch (RuntimeException e) {
        		logger.debug("VS " + signalInfo + " not found");
				items.addSensorData(null);
        	}
        }
        toReturn.setGetScalarDataSeriesInBatchResult(items);
        return toReturn;
    }

	public org.tempuri.GetAggregateScalarDataInBatchResponse GetAggregateScalarDataInBatch(org.tempuri.GetAggregateScalarDataInBatch input) {
		org.tempuri.GetAggregateScalarDataInBatchResponse  toReturn = new  GetAggregateScalarDataInBatchResponse();
		ArrayOfSensorData items = new ArrayOfSensorData();
		for (String signalInfo: input.getSensorNames().getString()) {
			try {
				SignalRequest req = new SignalRequest(signalInfo);
				StringBuilder query = new StringBuilder("select AVG(TIMED) as TIMED, AVG(").append(req.getFieldName()).append(") as data from ").append(req.getVsName()).append(" where TIMED >= ").append(input.getStartTime().getTimeInMillis()).append(" AND TIMED <= ").append(input.getEndTime().getTimeInMillis());
				items.addSensorData(transformToSensorDataArray(req.getVsName(), query).getSensorData()[0]);
			}
			catch (RuntimeException e) {
				logger.debug("VS " + signalInfo + " not found");
				items.addSensorData(null);
			}
		}
		toReturn.setGetAggregateScalarDataInBatchResult(items);
		return toReturn;
	}

	class SignalRequest {
		private int signal_index = -1;
		private VSensorConfig conf;
		public SignalRequest(String req) {
			StringTokenizer st= new StringTokenizer(req,"@");
			if (st.countTokens()!=2)
				throw new RuntimeException("Bad request: correct format is sensorName@FieldID , Your (invalid) request is:"+req);
			String vsName = st.nextToken();
			this.signal_index= Integer.parseInt(st.nextToken());
			logger.debug("WS-REQUEST: VSNAME : "+vsName+",VSFIELD INDEX : "+signal_index);
			this.conf =Mappings.getVSensorConfig(vsName);
			if (signal_index>=conf.getOutputStructure().length)
				throw new RuntimeException("Bad request: vs-name="+vsName+", "+signal_index+">"+conf.getOutputStructure().length);
		}
		public String getFieldName() {
			return conf.getOutputStructure()[signal_index].getName();
		}
		public String getVsName() {
			return conf.getName();
		}
	}

	private  ArrayOfSensorData transformToSensorDataArray(String vsName, StringBuilder query) {
		boolean is_binary_linked= true;
		if (query.toString().replaceAll(" ","").toLowerCase().indexOf("avg(")>0)
			is_binary_linked = false;
		return transformToSensorDataArray(vsName, query, is_binary_linked);
	}

	private  ArrayOfSensorData transformToSensorDataArray(String vsName, StringBuilder query, boolean is_binary_linked) {
		logger.debug("QUERY : "+query);
		ArrayOfSensorData toReturn = new ArrayOfSensorData();
		try {
			DataEnumerator output = null;
			output = Main.getStorage(vsName).executeQuery(query, is_binary_linked);
			SensorData data = new SensorData();
			ArrayOfDateTime arrayOfDateTime = new ArrayOfDateTime();
			ArrayList<Double> sensor_readings = new ArrayList();
			while(output.hasMoreElements()) {
				StreamElement se = output.nextElement();
				Calendar timestamp = Calendar.getInstance();
				timestamp.setTimeInMillis(se.getTimeStamp());
				arrayOfDateTime.addDateTime(timestamp);
				sensor_readings.add(Double.parseDouble(se.getData()[0].toString()));
			}
			data.setSensorType(5);//Vector
			data.setDataType(1);// Generic
			data.setTimestamps(arrayOfDateTime);
			ArrayOfDouble arrayOfDouble = new ArrayOfDouble();
			arrayOfDouble.set_double(ArrayUtils.toPrimitive(sensor_readings.toArray(new Double[] {})));
			data.setData(arrayOfDouble);
			toReturn.addSensorData(data);
		}catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}

		return toReturn;
	}



	/********************************************************************************************************/
	/********************************* AUTO GENERATED METHODS START FROM HERE *******************************/
	/********************************************************************************************************/

        /**
         * Auto generated method signature
         * Deletes an existing sensor
                                     * @param deleteSensor
         */

                 public org.tempuri.DeleteSensorResponse DeleteSensor
                  (
                  org.tempuri.DeleteSensor deleteSensor
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#DeleteSensor");
        }


        /**
         * Auto generated method signature
         * Stores a series of sensor data in datahub
                                     * @param storeScalarDataBatch
         */

                 public org.tempuri.StoreScalarDataBatchResponse StoreScalarDataBatch
                  (
                  org.tempuri.StoreScalarDataBatch storeScalarDataBatch
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#StoreScalarDataBatch");
        }


        /**
         * Auto generated method signature
         * Gets data from one component sensor of a vector sensor
                                     * @param getLatestVectorDataByComponentIndex
         */

                 public org.tempuri.GetLatestVectorDataByComponentIndexResponse GetLatestVectorDataByComponentIndex
                  (
                  org.tempuri.GetLatestVectorDataByComponentIndex getLatestVectorDataByComponentIndex
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetLatestVectorDataByComponentIndex");
        }


        /**
         * Auto generated method signature
         * Returns the latest image data reported by a sensor
                                     * @param getLatestBinarySensorData
         */

                 public org.tempuri.GetLatestBinarySensorDataResponse GetLatestBinarySensorData
                  (
                  org.tempuri.GetLatestBinarySensorData getLatestBinarySensorData
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetLatestBinarySensorData");
        }


        /**
         * Auto generated method signature
         * Modifies location of a sensor
                                     * @param updateSensorLocation
         */

                 public org.tempuri.UpdateSensorLocationResponse UpdateSensorLocation
                  (
                  org.tempuri.UpdateSensorLocation updateSensorLocation
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#UpdateSensorLocation");
        }


        /**
         * Auto generated method signature
         * Gets the latest data published by a sensor
                                     * @param getLatestScalarData
         */

                 public org.tempuri.GetLatestScalarDataResponse GetLatestScalarData
                  (
                  org.tempuri.GetLatestScalarData getLatestScalarData
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetLatestScalarData");
        }


        /**
         * Auto generated method signature
         * Registers a new sensor
                                     * @param registerSensor
         */

                 public org.tempuri.RegisterSensorResponse RegisterSensor
                  (
                  org.tempuri.RegisterSensor registerSensor
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#RegisterSensor");
        }


        /**
         * Auto generated method signature
         * Stores data from a vector sensor
                                     * @param storeVectorData
         */

                 public org.tempuri.StoreVectorDataResponse StoreVectorData
                  (
                  org.tempuri.StoreVectorData storeVectorData
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#StoreVectorData");
        }


        /**
         * Auto generated method signature
         * Retreives metadata of an existing sensor
                                     * @param getSensorByPublisherAndName
         */

                 public org.tempuri.GetSensorByPublisherAndNameResponse GetSensorByPublisherAndName
                  (
                  org.tempuri.GetSensorByPublisherAndName getSensorByPublisherAndName
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetSensorByPublisherAndName");
        }


        /**
         * Auto generated method signature
         * Gets data from a vector sensor
                                     * @param getLatestVectorData
         */

                 public org.tempuri.GetLatestVectorDataResponse GetLatestVectorData
                  (
                  org.tempuri.GetLatestVectorData getLatestVectorData
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetLatestVectorData");
        }


        /**
         * Auto generated method signature
         * Send binary sensor data such as images, sound or video. Data are treated as a binary file. Time parameter is the time stamp of the first data.
                                     * @param storeBinaryData
         */

                 public org.tempuri.StoreBinaryDataResponse StoreBinaryData
                  (
                  org.tempuri.StoreBinaryData storeBinaryData
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#StoreBinaryData");
        }


        /**
         * Auto generated method signature
         * Gets the data published by a sensor within a specified time window
                                     * @param getScalarDataSeries
         */

                 public org.tempuri.GetScalarDataSeriesResponse GetScalarDataSeries
                  (
                  org.tempuri.GetScalarDataSeries getScalarDataSeries
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetScalarDataSeries");
        }


        /**
         * Auto generated method signature
         * Returns metadata of all the sensors published by a given publisher
                                     * @param getSensorsByPublisher
         */

                 public org.tempuri.GetSensorsByPublisherResponse GetSensorsByPublisher
                  (
                  org.tempuri.GetSensorsByPublisher getSensorsByPublisher
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#GetSensorsByPublisher");
        }


        /**
         * Auto generated method signature
         * Returns string representation of a sensor data
                                     * @param dataToString
         */

                 public org.tempuri.DataToStringResponse DataToString
                  (
                  org.tempuri.DataToString dataToString
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#DataToString");
        }


        /**
         * Auto generated method signature
         * Stores data from one component sensor of a vector sensor
                                     * @param storeVectorDataByComponentIndex
         */

                 public org.tempuri.StoreVectorDataByComponentIndexResponse StoreVectorDataByComponentIndex
                  (
                  org.tempuri.StoreVectorDataByComponentIndex storeVectorDataByComponentIndex
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#StoreVectorDataByComponentIndex");
        }


        /**
         * Auto generated method signature
         * Registers a new vector sensor
                                     * @param registerVectorSensor
         */

                 public org.tempuri.RegisterVectorSensorResponse RegisterVectorSensor
                  (
                  org.tempuri.RegisterVectorSensor registerVectorSensor
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#RegisterVectorSensor");
        }


        /**
         * Auto generated method signature
         * Stores a sensordata in datahub
                                     * @param storeScalarData
         */

                 public org.tempuri.StoreScalarDataResponse StoreScalarData
                  (
                  org.tempuri.StoreScalarData storeScalarData
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#StoreScalarData");
        }


        /**
         * Auto generated method signature
         * Deletes an existing vector sensor
                                     * @param deleteVectorSensor
         */

                 public org.tempuri.DeleteVectorSensorResponse DeleteVectorSensor
                  (
                  org.tempuri.DeleteVectorSensor deleteVectorSensor
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#DeleteVectorSensor");
        }

/**
         * Auto generated method signature
         *
                                     * @param setSensorData
         */

                 public org.tempuri.SetSensorDataResponse SetSensorData
                  (
                  org.tempuri.SetSensorData setSensorData
                  )
            {
                //TODO : fill this with the necessary business logic
                throw new  java.lang.UnsupportedOperationException("Please implement " + this.getClass().getName() + "#SetSensorData");
        }
               

    }
    