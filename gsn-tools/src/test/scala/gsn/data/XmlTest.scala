package gsn.data

import org.scalatest.FunSpec
import org.scalatest.Matchers
import gsn.data.format.XmlSerializer

class XmlTest extends FunSpec with Matchers {
    
  describe("xml write"){

    val fields=Seq(
        Sensing("timbo",Output("time","s1",DataUnit("s","s"),LongType,None)),
        Sensing("air-temperature",Output("temp","s1",DataUnit("C","C"),DoubleType,None)),
        Sensing("relative-humidity",Output("humid","s1",DataUnit("Perc","Perc"),DoubleType,None)))
        
        
    val values:Seq[Seq[Any]]=Seq(
        Array(11,36.5,98.2),
        Array(12,31.5,92.2),
        Array(13,30.5,94.2),
        Array(14,29.5,97.2),
        Array(15,32.5,95.2))    
    val s=new Sensor("pipo",fields,null,Map("description"->"chochos"))
    
    it("should serialize it"){
      val ss=XmlSerializer.ser(s, Seq())
      println(ss)
      
    }
  }
}