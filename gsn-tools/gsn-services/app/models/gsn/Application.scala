package models.gsn

import java.sql.ResultSet
import scala.util.Try
import controllers.gsn.Global

case class Application(_pk: Int, name: String, id: String, secret: String, user: Int, code: Option[String], redirect: Option[String]) extends Model(_pk)

object ApplicationManager extends AbstractManager[Application] {
  
  override def getTableName: String = "AC_OAUTH_APP"
  
  override def buildFromResultSet(rs: ResultSet): Try[Application] = 
    Try(Application(rs.getInt(0),rs.getString(1), rs.getString(2), rs.getString(3),rs.getInt(4),Option(rs.getString(5)),Option(rs.getString(6))))
  
  override def stringForInsert(app: Application): String = 
    s"""'${app.name}','${app.id}','${app.secret}','${app.user}','${app.code.getOrElse("NULL")}','${app.redirect.getOrElse("NULL")}'"""
    
  override def stringForUpdate(app: Application): String = 
    s"""NAME='${app.name}',ID='${app.id}',SECRET='${app.secret}',USER='${app.user}',CODE='${app.code.getOrElse("NULL")}',REDIRECT='${app.redirect.getOrElse("NULL")}'""" 
   
  // additional helper methods
    
  def logIn(id: String, secret: Option[String]): Boolean = 
    ! selectWithFilter(("ID",id),("SECRET",secret.getOrElse("NULL"))).isEmpty

}