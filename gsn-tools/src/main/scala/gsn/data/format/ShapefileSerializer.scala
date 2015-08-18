package gsn.data.format

import gsn.data._
import org.geotools.data.shapefile.ShapefileDataStoreFactory
import java.io.File
import java.io.FileInputStream
import org.geotools.data.DataUtilities
import org.geotools.feature.simple.SimpleFeatureBuilder
import java.util.ArrayList
import org.opengis.feature.simple.SimpleFeature
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.DefaultTransaction
import com.vividsolutions.jts.geom.Point
import org.geotools.data.simple.SimpleFeatureStore
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.Coordinate
import java.util.UUID
import java.nio.file.Paths
import java.nio.file.Files
import java.io.ByteArrayOutputStream
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
import java.nio.file.Path
import org.slf4j.LoggerFactory

object ShapefileSerializer extends DataSerializer{
  private val log=LoggerFactory.getLogger(getClass)
  override def ser(data:Seq[SensorData],props:Seq[String],latest:Boolean)=
    toShp(data,props,latest)

  override def ser(data:SensorData,props:Seq[String],latest:Boolean)=
    toShp(data,props).toString

  private val extensions=Seq("dbf","shp","shx","prj")  
    
  def toShp(data:SensorData,props:Seq[String])=throw new NotImplementedError
  
  def toShp(data:Seq[SensorData],props:Seq[String],latest:Boolean)={
    val id=UUID.randomUUID
    val shpName=s"${id}.shp"
    log.debug("Creating temp file "+id)
    val ff=Files.createTempFile(id.toString, ".shp")
    log.debug("Created temp file "+ff)
    //val shp=new File(shpName)
    val dataStoreFac=new ShapefileDataStoreFactory
    val params=new java.util.HashMap[String,java.io.Serializable]()
    params.put("url", ff.toUri().toURL())
    params.put("create spatial index", java.lang.Boolean.TRUE.toString)

    log.debug("Init shp store "+ff.toUri.toURL)
    
    val store=dataStoreFac.createNewDataStore(params)    
    
    val propStr=props.map(p=>"$p:String").mkString(",")
    
    val TYPE = DataUtilities.createType("Location",
                "the_geom:Point:srid=4326,vs_name:String," + propStr)// <- the geometry attribute: Point type
                                
    
    val fBuilder = new SimpleFeatureBuilder(TYPE)
    val features=new ArrayList[SimpleFeature]()
    
    data.foreach{d=>
      val loc=d.sensor.location   
      if (loc.latitude.isDefined && loc.longitude.isDefined ){
        
        val point=toPoint(loc)        
        fBuilder.add(point)
        fBuilder.add(d.sensor.name)
        props.foreach{p=>
          fBuilder.add(d.sensor.properties.getOrElse(p, "") )
        }
        //fBuilder.add(d.sensor.name )
        //fBuilder.add(2342)
        val ff=fBuilder.buildFeature(null)
        features.add(ff)
      }
    }

    val collection = new ListFeatureCollection(TYPE, features);
    store.createSchema(TYPE)
    val trans=new DefaultTransaction("create")
    val typeName=store.getTypeNames()(0)
    val source=store.getFeatureSource(typeName)
    val fStore=source.asInstanceOf[SimpleFeatureStore]
    fStore.setTransaction(trans)
    fStore.addFeatures(collection)
    trans.commit
    trans.close

    zip(ff)
  }
  
  private def toBytes(path:Path)={
    Files.readAllBytes(path)        
  }
  
  private def zip(shpPath:Path)={
    val shpId=shpPath.getFileName.toString.replace(".shp","")
    val parent=shpPath.getParent.toString
    val fNames=extensions.map(ext=>shpId+"."+ext)
    val baos=new ByteArrayOutputStream
    val zos=new ZipOutputStream(baos)
    fNames.foreach{name=>
      val entry=new ZipEntry(name)
      zos.putNextEntry(entry)
      val path=Paths.get(parent+"/"+name)   
      val bt=toBytes(path)
      Files.delete(path)
      zos.write(bt)
      zos.closeEntry
    }
    zos.finish
    zos.close
    baos.toByteArray

  }
  
  
  private def toPoint(loc:Location):Point={
    val gf=new GeometryFactory()
    gf.createPoint(new Coordinate(loc.latitude.get,loc.longitude.get))    
  }
}