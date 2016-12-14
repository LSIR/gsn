/**
* Global Sensor Networks (GSN) Source Code
* Copyright (c) 2006-2016, Ecole Polytechnique Federale de Lausanne (EPFL)
* 
* This file is part of GSN.
* 
* GSN is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* GSN is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with GSN.  If not, see <http://www.gnu.org/licenses/>.
* 
* File: src/ch/epfl/gsn/data/bson/BsonConverter.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.bson

import ch.epfl.gsn.data._
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
            doc.getAs[Double]("altitude"),
            None, None, None)
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