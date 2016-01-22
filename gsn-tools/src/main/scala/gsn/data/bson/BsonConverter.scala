package gsn.data.bson

import gsn.data._
import reactivemongo.bson._

object BsonConverter{

  implicit object PlatformWriter extends BSONDocumentWriter[Platform]{    
    def write(p: Platform): BSONDocument =      
      BSONDocument("name"->p.name,
          "latitude"->p.location.latitude, 
          "longitude"->p.location.longitude, 
          "altitude"->p.location.altitude )                          
  }
  
  implicit object PlatformReader extends BSONDocumentReader[Platform]{
    def read(doc:BSONDocument):Platform=Platform(
        doc.getAs[String]("name").get,
        Location(doc.getAs[Double]("latitude"),
            doc.getAs[Double]("longitude"),
            doc.getAs[Double]("altitude"))
    )
  }

  implicit object OutputWriter extends BSONDocumentWriter[Output]{    
    def write(o: Output): BSONDocument =      
      BSONDocument("name"->o.fieldName ,
          "stream"->o.stream  , 
          "type"->o.dataType.name , 
          "unit"->o.unit.code)                          
  }
  
  implicit object OutputReader extends BSONDocumentReader[Output]{
    def read(doc:BSONDocument):Output=Output(
        doc.getAs[String]("name").get,
        doc.getAs[String]("stream").get,
        DataUnit(doc.getAs[String]("unit").get),
        DataType(doc.getAs[String]("type").get)        
    )
  }
  
  implicit object SensorWriter extends BSONDocumentWriter[Sensor]{    
    def write(s: Sensor): BSONDocument = {
      val vsensor=Seq("vsname"->BSONString(s.name),
          "platform"->BSON.write(s.platform),
          "fields" -> BSON.write(s.fields) )
      
      BSONDocument(vsensor ++   
        s.properties.map(p=>p._1 ->BSONString(p._2)))                           
    }    
  }
  
  implicit object SensorReader extends BSONDocumentReader[Sensor]{
    def read(doc:BSONDocument):Sensor=Sensor(
        doc.getAs[String]("vsname").get,
        doc.getAs[List[Output]]("fields").get.map{f=>
          Sensing(f.fieldName,f)
        }.toSeq,        
        doc.getAs[Platform]("platform").get,
        Map()
    )
  }
  

  implicit object SensorStatsWriter extends BSONDocumentWriter[SensorStats]{    
    def write(s: SensorStats): BSONDocument =      
      BSONDocument("start-datetime"->s.start,
          "end-datetime"->s.end,          
          "rate"->s.rate  
         )                          
  }

}