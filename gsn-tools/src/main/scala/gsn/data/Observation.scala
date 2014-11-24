package gsn.data

import java.util.Date

case class Observation(value:ObservationValue,time:Date,sensor:Sensor,obsProperty:String,feature:String) 

case class ObservationValue(value:Any,dataType:DataType)

case class TimeSeries(output:Output,series:Seq[(Any,Any)])


case class SensorData(ts:Seq[TimeSeries],s:Sensor)