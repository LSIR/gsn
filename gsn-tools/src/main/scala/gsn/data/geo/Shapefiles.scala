package gsn.data.geo

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