package gsn.data.format

import gsn.data.DataSerializer
import gsn.data._
import java.io.StringWriter
import java.io.ByteArrayOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.io.FileOutputStream

object CsvSerializer extends DataSerializer{
  
  override def ser(data:Seq[SensorData],props:Seq[String],latest:Boolean)=
    toCsv(data,props,latest).toString

  override def ser(data:SensorData,props:Seq[String],latest:Boolean)=
    toCsv(data,props).toString

  def toCsv(data:Seq[SensorData],props:Seq[String],latest:Boolean)={
    val sw=new StringWriter
    //val hasVals= !data.ts.isEmpty
    val valsHead=if (latest) ",values" else ""
    sw.append("#vs_name,"+props.mkString(",")+",fields,units,types"+valsHead+System.lineSeparator)
    data.foreach{s=>
      sw.append(sensorDataToCsv(s,props,latest)+System.lineSeparator())
    }
    sw
  }
  private def toString(s:SensorData,props:Seq[String])={
    val output=s.ts.map(t=>t.output)
    val prop=props.map(p=>"\""+encodeBreaks(s.sensor.properties.getOrElse(p,""))+"\"").mkString(",")
    s"${s.sensor.name},$prop"+
    ",\""+output.map(f=>f.fieldName).mkString("|")+"\""+
    ",\""+output.map(f=>f.unit.code).mkString("|")+"\""+
    ",\""+output.map(f=>f.dataType.name).mkString("|")+"\""    
  }
  
  def sensorDataToCsv(data:SensorData,props:Seq[String],withLatest:Boolean=false)={
    //val valueNames="latestValues("+data.latest.map(_._1.fieldName).mkString("|")+")"
    //"#vs_name,"+props.mkString(",")+",fields,units,types,"+valueNames+System.lineSeparator
    if (withLatest)
      toString(data,props)+","+data.latest.map(_._2).mkString("|")
    else toString(data,props)
  }

  private def encodeBreaks(s:String)=s.replaceAll("(\r\n)|\r|\n", "\\\\n")

  
  
  
  def toCsv(data:SensorData,
      valueNames:Seq[String]=Seq()):StringWriter={
    val sw=new StringWriter
    def head(key:String,value:Any)=
      sw.append("# "+key+":"+value+System.lineSeparator)
    
    head("vs_name",data.sensor.name)
    data.sensor.properties.foreach(p=>head(p._1,encodeBreaks(p._2)))
    //val fields=data.sensor.fields.filter(f=>valueNames.isEmpty || valueNames.contains(f.fieldName ))
    val fields=data.ts.map(_.output)
    
    head("fields",fields.map{_.fieldName}.mkString(","))
    head("units",fields.map{_.unit.code}.mkString(","))
    head("types",fields.map{_.dataType.name}.mkString(","))
    if (!data.ts.isEmpty){
	    println("drimp "+data.ts.head.series.size+" "+fields.size)
	    val si=data.ts.head.series.size-1
	    (0 to si).foreach{i=>
	      //sw.append(data.time(i)+",")
	      val pp=(0 to fields.size-1).map{j=>
	        data.ts(j).series (i)        
	      }.mkString(",")
	      sw.append(pp+System.lineSeparator)
	      //sw.append(pp)  
	    }
    }
    
    sw  
    
  }
  
  
  def serZip(data:Seq[SensorData],props:Seq[String],latest:Boolean)={
    val baos=new ByteArrayOutputStream
    val zos=new ZipOutputStream(baos)
    data.foreach{d=>
      val entry=new ZipEntry(d.sensor.name+".csv")
      zos.putNextEntry(entry)
      zos.write(toCsv(d).toString.getBytes())
      zos.closeEntry
    }
    zos.finish()
    zos.close()
    baos.toByteArray()
  }
    

}