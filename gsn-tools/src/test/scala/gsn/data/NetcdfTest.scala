package gsn.data

import org.scalatest.FunSpec
import org.scalatest.Matchers
import gsn.data.netcdf.NetCdf

class NetcdfTest extends FunSpec with Matchers {
    
  describe("netcdf write"){
    
    val fields=Seq(Field("timed",LongDT,DataUnit("s","s"),"timbo"),
        Field("temp",DoubleDT,DataUnit("C","C"),"air-temperature"),
        Field("humid",DoubleDT,DataUnit("Perc","Perc"),"relative-humidity"))
        
        
    val values:Seq[Array[Any]]=Seq(
        Array(11,36.5,98.2),
        Array(12,31.5,92.2),
        Array(13,30.5,94.2),
        Array(14,29.5,97.2),
        Array(15,32.5,95.2))    
    val s=new Sensor("pipo",fields,null,Map("description"->"chochos"),values)
    
    it("should serialize it"){
          NetCdf.serialize(s)
      //nc.testCreate

    }
  }
}