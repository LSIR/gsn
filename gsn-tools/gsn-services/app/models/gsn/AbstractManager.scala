package models.gsn

import gsn.data.DataStore
import controllers.gsn.Global
import java.sql.ResultSet
import scala.util.Try
import scala.util.Success

trait Model {def _pk:Int}

abstract class AbstractManager[T <: Model] {
  
  def getTableName: String
  def buildFromResultSet(rs: ResultSet): Try[T]
  def stringForInsert(obj: T): String
  def stringForUpdate(obj: T): String
  
  def getByPk(pk: Int): Option[T] = {
      if (pk == -1)
          None
      else{
          val q=s"""SELECT * FROM $getTableName WHERE PK='$pk'"""
          Global.ds.withSession { implicit session => 
              val rs=session.conn.createStatement.executeQuery(q)
              if (rs.next){
                  buildFromResultSet(rs).toOption
              }else{
                  None
              }
          }
      }
  }
  
  def updateOrCreate(obj: T): Boolean =
      getByPk(obj._pk) match {
      case s:Some[T] => {
        val s = s"""UPDATE $getTableName SET (${stringForUpdate(obj)}) WHERE PK='${obj._pk}'"""
        Global.ds.withSession { implicit session => session.conn.createStatement.execute(s) }
      }
      case None => {
        val s = s"""INSERT INTO $getTableName VALUES (${stringForInsert(obj)})"""
        Global.ds.withSession { implicit session => session.conn.createStatement.execute(s) }
        
      }
  }
  
  def selectWithFilter(conditions: (String, Any)*):List[T] = {
       val q=s"""SELECT * FROM $getTableName WHERE ${conditions.map((t:(String,Any))=>s"""${t._1}='${t._2}'""").mkString(" AND ")}"""
       Global.ds.withSession { implicit session => 
           val rs=session.conn.createStatement.executeQuery(q)
           Iterator.continually(buildFromResultSet(rs).toOption).takeWhile{x => rs.next}.flatten.toList
       }
  }
  
  def deleteByPk(pk:Int): Boolean = {
    val q=s"""DELETE FROM $getTableName WHERE PK='$pk'"""
    Global.ds.withSession { implicit session => 
        session.conn.createStatement.executeQuery(q).rowDeleted()
    }
   
  }
}