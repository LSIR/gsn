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
* File: src/ch/epfl/gsn/data/geo/Shapefiles.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data.geo

import java.io.File
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import java.io.FileInputStream
import org.geotools.geojson.geom.GeometryJSON
import org.geotools.geojson.feature.FeatureJSON
import org.opengis.feature.simple.SimpleFeatureType
import org.geotools.data.DefaultTransaction
import org.geotools.data.simple.SimpleFeatureStore
import org.geotools.feature.FeatureCollection
import org.opengis.feature.simple.SimpleFeature
import com.vividsolutions.jts.geom.Point
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.data.DataUtilities
import java.util.ArrayList
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.memory.MemoryDataStore
import collection.JavaConversions._
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.Coordinate

object Shapefiles {
  
  def main (args:Array[String])=
  {
    toShp
  }
  def toShp={
    val shp=new File("test.shp")
    val dataStoreFac=new ShapefileDataStoreFactory
    val params=new java.util.HashMap[String,java.io.Serializable]()
    params.put("url", shp.toURI().toURL())
    params.put("create spatial index", java.lang.Boolean.TRUE.toString)
    
    val store=dataStoreFac.createNewDataStore(params)
    //val store=new MemoryDataStore
    
    //val in=new FileInputStream("sensors.json")
    //val decimals=15
    //val gjson=new GeometryJSON()
    
    //val fjson=new FeatureJSON() //gjson)
    
    //val fc=fjson.readFeatureCollection(in).asInstanceOf[FeatureCollection[SimpleFeatureType,SimpleFeature]]
    //val tt=fc.getSchema().asInstanceOf[SimpleFeatureType]
    //val gFac=JTSFactoryFinder.getGeometryFactory()
    val TYPE = DataUtilities.createType("Location",
                "the_geom:Point:srid=4326," + // <- the geometry attribute: Point type
                "name:String," +   // <- a String attribute
                "number:Integer")   // a number attribute
    
    //val fBuilder = new SimpleFeatureBuilder(TYPE)
    val features=new ArrayList[SimpleFeature]()
    
    //println(fc.size())

    val gf=new GeometryFactory
      val p=gf.createPoint(new Coordinate(2,4))
      val objs:Array[Object]=Array(p,"dibibi","213",2332.toString)      
          val TYP = DataUtilities.createType("Location",
                "the_geom:Point:srid=4326,name:String,number:Integer,tapa:Integer")    
      val ff=SimpleFeatureBuilder.build(TYP, objs,"dibi")
      features.add(ff)
      val p2=gf.createPoint(new Coordinate(3,5))
      val objs2:Array[Object]=Array(p2,"dibibi2","213",2332.toString,"3232")      
          val TYP2 = DataUtilities.createType("Location",
                "the_geom:Point:srid=4326,name:String,brito:Integer,tapa:Integer,pit:Integer")    
      val ff2=SimpleFeatureBuilder.build(TYP2, objs2,"dibi2")
      features.add(ff2)
    
    val collection = new ListFeatureCollection(TYPE, features);
    //store.createSchema(TYPE)
    val trans=new DefaultTransaction("create")
    val typeName=store.getTypeNames()(0)
    val source=store.getFeatureSource(typeName)
    //store.get
    val fStore=source.asInstanceOf[SimpleFeatureStore]
    fStore.setTransaction(trans)
    fStore.addFeatures(collection)
    trans.commit
    trans.close
    
    
    
    
    
    
  }
  
  def createFeature(){
    
  }
}