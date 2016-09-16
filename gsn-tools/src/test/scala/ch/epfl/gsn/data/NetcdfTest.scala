package ch.epfl.gsn.data

import org.scalatest.FunSpec
import org.scalatest.Matchers

import ch.epfl.gsn.data.DoubleType;
import ch.epfl.gsn.data.LongType;
import ch.epfl.gsn.data.Sensor;
import ch.epfl.gsn.data.format.NetCdfSerializer

class NetcdfTest extends FunSpec with Matchers {
    
  describe("netcdf write"){

    val fields=Seq(
        Sensing("timbo",Output("time","s1",DataUnit("s","s"),LongType)),
        Sensing("air-temperature",Output("temp","s1",DataUnit("C","C"),DoubleType)),
        Sensing("relative-humidity",Output("humid","s1",DataUnit("Perc","Perc"),DoubleType)))
        
        
    val values:Seq[Seq[Any]]=Seq(
        Array(11,36.5,98.2),
        Array(12,31.5,92.2),
        Array(13,30.5,94.2),
        Array(14,29.5,97.2),
        Array(15,32.5,95.2))    
    val s=new Sensor("pipo",fields,null,Map("description"->"chochos"))
    
    it("should serialize it"){
          NetCdfSerializer.serialize(s,values)
      //nc.testCreate

    }
  }
}