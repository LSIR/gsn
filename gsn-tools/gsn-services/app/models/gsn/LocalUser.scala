package models.gsn

import java.sql.ResultSet
import scala.util.Try
import controllers.gsn.Global
import org.mindrot.jbcrypt.BCrypt

case class LocalUser(_pk: Int, user: Int, username: String, password: String, firstname: String, lastname: String, email: String) extends Model(_pk)

object LocalUserManager extends AbstractManager[LocalUser] {
  
    def getTableName: String = "ACUSER"
    
    def buildFromResultSet(rs: ResultSet): Try[LocalUser] = 
      Try(LocalUser(rs.getInt(0),rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6)))
    
    def stringForInsert(u: LocalUser): String =
      s"""'${u.user}' ,'${u.username}','${u.password}','${u.firstname}','${u.lastname}','${u.email}'"""
    
    def stringForUpdate(u: LocalUser): String = 
      s"""USER='${u.user}' ,USERNAME='${u.username}',PASSWORD='${u.password}',FIRSTNAME='${u.firstname}',LASTNAME='${u.lastname}',EMAIL='${u.email}'"""
 
     // additional helper methods
      
    def logIn(username: String, password: String):Option[LocalUser]={
      val u = selectWithFilter(("USERNAME",username)).headOption
      u.collect(new PartialFunction[LocalUser,LocalUser]{
        def apply(u:LocalUser)=u
        def isDefinedAt(u:LocalUser)=BCrypt.checkpw(password, u.password)})
    }
  
}
