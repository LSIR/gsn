package gsn.data

import gsn.data
import gsn.config.GsnConf
import com.typesafe.config.ConfigFactory

object SensorDataBaseTest {
  def main(args:Array[String])={
      val gsnConf=GsnConf.load(ConfigFactory.load.getString("gsn.config"))
  val ds =new DataStore(gsnConf)        
    val vsname="wannengrat_wan7"
    val fields=Seq(
        Sensing("record",Output("record",vsname,DataUnit("s","s"),LongType)),
        Sensing("air_temperature",Output("air_temperature",vsname,DataUnit("C","C"),DoubleType)),
        Sensing("relative_humidity",Output("relative_humidity",vsname,DataUnit("Perc","Perc"),DoubleType)))
        
    val sensor=SensorInfo(new Sensor(vsname,fields,null,Map()),None,None)
    val res=SensorDatabase.query(sensor, Seq(), Seq(), Some(6), None)
    println(res.ts.size)
  }
}