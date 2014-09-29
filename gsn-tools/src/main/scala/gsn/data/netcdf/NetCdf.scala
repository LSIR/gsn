package gsn.data.netcdf
import ucar.nc2.NetcdfFileWriter
import ucar.nc2.Attribute
import gsn.data.Sensor
import ucar.ma2.DataType
import ucar.ma2.ArrayDouble
import java.util.UUID
import java.nio.file.Files
import java.nio.file.Paths

object NetCdf {
  val timeName="time"
  def papa={
    
  }  
    
  def serialize(s:Sensor,timeFieldName:String="time")={
    val id=UUID.randomUUID
    implicit val w = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, id+".nc", null)
    addAttributes(Map(
        "title"->s.properties.getOrElse("description",""),
        "institution"-> "",
        "source"-> "","history"->"",
        "references"-> "","comment"-> ""))
        
    val dataSize=s.values.size    
    val timeDim = w.addDimension(null, timeName, dataSize) 
    val dataFields=s.fields
    if (dataFields.find(_.name==timeFieldName)==None) 
      throw new IllegalArgumentException("Invalid sensor structure: missing time field: "+timeFieldName)
    val fieldArrays=dataFields.map(f=>new ArrayDouble.D1(dataSize))
    dataFields.foreach{f=>
      
      if (f.name==timeFieldName){
        val fieldVar=w.addVariable(null, timeName,DataType.INT,timeName)
        fieldVar.addAttribute(new Attribute("units",f.unit.code))        
      }
      else {
        val fieldVar=w.addVariable(null, f.name,DataType.DOUBLE,timeName)
        if (f.unit.code!=null)
          fieldVar.addAttribute(new Attribute("units",f.unit.code))
        if (f.obsproperty!=null)
          fieldVar.addAttribute(new Attribute("long_name",f.obsproperty))
      }
    }  
    w.create

    var i=0
    s.values.foreach{v=>
      var j=0
      v.foreach{value=>
        fieldArrays(j).setObject(i, value)
        j+=1        
      }   
      i+=1
    }
    
    i=0
    dataFields.foreach{f=>
      if (f.name==timeFieldName)
        w.write(w.findVariable(timeName), fieldArrays(i))
      else
        w.write(w.findVariable(f.name), fieldArrays(i))
      i+=1
    }
       
    w.close
    w.getNetcdfFile().writeCDL(System.out, false)
    val p=Paths.get(id+".nc")
    val bytes=Files.readAllBytes(Paths.get(id+".nc"))    
    Files.delete(Paths.get(id+".nc"))
    bytes
  }
  
  private def addAttributes(atts:Map[String,String])(implicit w:NetcdfFileWriter)={
    atts.foreach{case (k,v)=>
      w.addGroupAttribute(null, new Attribute(k,v))
    }
  }

}