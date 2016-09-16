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
* File: src/ch/epfl/gsn/data/rdf/SsnModel.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.rdf

import ch.epfl.gsn.data._
import com.hp.hpl.jena.rdf.model.Model
import com.hp.hpl.jena.rdf.model.ModelFactory
import ch.epfl.gsn.vocab.Ssn
import com.hp.hpl.jena.vocabulary.RDF
import com.hp.hpl.jena.vocabulary.RDFS
import com.hp.hpl.jena.vocabulary.DCTerms
import ch.epfl.gsn.vocab.Qu
import com.hp.hpl.jena.rdf.model.ResourceFactory
import ch.epfl.gsn.vocab.QuUnit

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