package gsn.data

import java.util.Date

case class Observation(value:ObservationValue,time:Date,sensor:Sensor,
    obsProperty:String,feature:String) 

case class ObservationValue(value:Any,dataType:DataType)

case class Series(output:Output,series:Seq[Any]){
  def asDoubles=series.iterator.map(d =>d.asInstanceOf[Double])
  def asLongs=series.iterator.map(d =>d.asInstanceOf[Long])
}

class TimeSeries(output:Output,series:Seq[Any], val time:Seq[Long]) extends Series(output,series){
  val iterator=(asDoubles zip time.iterator)
}

class NumericalTimeSeries(output:Output,series:Seq[Double]) extends Series(output,series)

case class SensorData(ts:Seq[Series],sensor:Sensor,stats:SensorStats=EmptyStats){
  lazy val latest=
    if (ts.isEmpty || ts.head.series.isEmpty ) Seq()
    else {
      ts.map{ t=>(t.output ,t.series.last) }
    }
}