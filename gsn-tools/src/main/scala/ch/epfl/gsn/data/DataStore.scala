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
* File: src/ch/epfl/gsn/data/DataStore.scala
*
* @author Jean-Paul Calbimonte
*
*/
package ch.epfl.gsn.data

import com.typesafe.config.ConfigFactory
import scala.slick.jdbc.StaticQuery.interpolation
import scala.slick.jdbc.GetResult
import scala.slick.jdbc.JdbcBackend._
import scala.collection.mutable.ArrayBuffer
import ch.epfl.gsn.config.VsConf
import ch.epfl.gsn.config.GsnConf
import collection.JavaConversions._
import org.slf4j.LoggerFactory
import ch.epfl.gsn.config.StorageConf
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