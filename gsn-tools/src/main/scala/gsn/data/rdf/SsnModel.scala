package gsn.data.rdf

import gsn.data._
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import gsn.vocab.Ssn
import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.vocabulary.RDFS
import com.hp.hpl.jena.vocabulary.DCTerms
import gsn.vocab.Qu
import com.hp.hpl.jena.rdf.model.ResourceFactory
import gsn.vocab.QuUnit

class SsnModel {

  val pref="http://example.com/ns#"
  val dbfield=ResourceFactory.createProperty(pref+"dateField")
  def toRdf(s:Sensor)={
    val m:Model = ModelFactory.createDefaultModel
    val sensorId=m.createResource(pref+s.name)
    m.add(sensorId, RDF.`type`, Ssn.Sensor)
    m.add(sensorId,DCTerms.identifier,s.name)
    //m.add()
    s.implements.foreach{sensing=>
    sensing.outputs.foreach{field=>
      val proc=m.createResource(pref+"proc"+s.name+"/"+field.fieldName  )
      val output=m.createResource(pref+"output"+s.name+"/"+field.fieldName )
      m.add(sensorId,Ssn.implements_,proc)
      m.add(proc,Ssn.forProperty,sensing.obsProperty )
      m.add(output,dbfield,field.fieldName )
      m.add(output,Qu.unit,field.unit.code  )
      m.add(proc,Ssn.hasOutput,output)      
    }
    }
    
    m.write(System.out)
  }
}