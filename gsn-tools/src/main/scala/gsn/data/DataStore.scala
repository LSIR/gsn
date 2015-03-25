package gsn.data

import com.typesafe.config.ConfigFactory
import scala.slick.jdbc.StaticQuery.interpolation
import scala.slick.jdbc.GetResult
import scala.slick.jdbc.JdbcBackend._
import scala.collection.mutable.ArrayBuffer
import gsn.config.VsConf
import gsn.config.GsnConf
import collection.JavaConversions._
import org.slf4j.LoggerFactory
import gsn.config.StorageConf
import com.mchange.v2.c3p0.ComboPooledDataSource
import com.mchange.v2.c3p0.C3P0Registry
import scala.util.Try
import javax.sql.DataSource

class DataStore(val gsn:GsnConf) {
  
  private val log =LoggerFactory.getLogger(classOf[DataStore])
  
  val db =
    Database.forDataSource(datasource("gsn",gsn.storageConf))

  def withSession[T](s: Session => T)=db.withSession[T](s)
  def withTransaction[T](s: Session => T)=db.withTransaction[T](s)  
  
  def datasource(name:String,store:StorageConf)={
    
    val ds=C3P0Registry.pooledDataSourceByName(store.url)
    if (ds!=null) ds
    else {
    	log debug("Create a new datasource: "+store)
	    val cpds = new ComboPooledDataSource(name)
	    cpds setDriverClass store.driver 
	    cpds setJdbcUrl store.url  
	    cpds setUser store.user 
	    cpds setPassword store.pass 
	    cpds setMinPoolSize 1 
	    cpds setAcquireIncrement 1 
	    cpds setMaxPoolSize 5
	    cpds
    }
  }
    
    
  
}