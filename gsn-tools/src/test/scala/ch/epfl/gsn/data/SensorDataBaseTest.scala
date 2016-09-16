package ch.epfl.gsn.data

import ch.epfl.gsn.data
import ch.epfl.gsn.config.GsnConf
import com.typesafe.config.ConfigFactory

import ch.epfl.gsn.data.DataStore;
import ch.epfl.gsn.data.DoubleType;
import ch.epfl.gsn.data.LongType;
import ch.epfl.gsn.data.Sensor;
import ch.epfl.gsn.data.SensorDatabase;

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