package gsn.data.format

import gsn.data.DataSerializer
import org.slf4j.LoggerFactory
import gsn.data.SensorData
import java.io.StringWriter

object EsriSerializer extends DataSerializer{
  private val log=LoggerFactory.getLogger(getClass)
  override def ser(data:Seq[SensorData],props:Seq[String],latest:Boolean)=
    toEsri(data.head).toString
  override def ser(data:SensorData,props:Seq[String],latest:Boolean)=
    toEsri(data).toString


  def toEsri(data:SensorData)={
    val sw=new StringWriter
    val fields=data.ts.map(_.output)
    
    if (!data.ts.isEmpty){
	    val si=data.ts.head.series.size-1
	    (0 to si).foreach{i=>
	      val pp=(0 to fields.size-1).map{j=>
	        if (fields(j).fieldName.equals("grid")){
	          val arr=data.ts(j).series(i)
	          println(arr.getClass.getCanonicalName())
	          val array=arr.asInstanceOf[Array[Array[_]]]
	          array.map(row=>row.mkString(" ")).mkString(System.lineSeparator)
	        }
	        else
  	          fields(j).fieldName+" "+data.ts(j).series(i)        
	      }.mkString(System.lineSeparator)
	      sw.append(pp+System.lineSeparator)
	    }
    }
    
    sw  
    
  }

}