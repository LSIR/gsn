package models.gsn

import java.sql.ResultSet
import scala.util.Try
import controllers.gsn.Global
import org.mindrot.jbcrypt.BCrypt
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser


case class LocalUser(_pk: Int, user: Int, username: String, password: String, firstname: String, lastname: String, email: String, verify: Option[String]) extends UsernamePasswordAuthUser(password,email) with Model

object LocalUserManager extends AbstractManager[LocalUser] {
  
    def getTableName: String = "ACUSER"
    
    def buildFromResultSet(rs: ResultSet): Try[LocalUser] = 
      Try(LocalUser(rs.getInt(0),rs.getInt(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),Some(rs.getString(7))))
    
    def stringForInsert(u: LocalUser): String =
      s"""'${u.user}' ,'${u.username}','${u.password}','${u.firstname}','${u.lastname}','${u.email}','${u.verify.getOrElse("NULL")}'"""
    
    def stringForUpdate(u: LocalUser): String = 
      s"""USER='${u.user}' ,USERNAME='${u.username}',PASSWORD='${u.password}',FIRSTNAME='${u.firstname}',LASTNAME='${u.lastname}',EMAIL='${u.email}',VERIFY='${u.verify.getOrElse("NULL")}'"""
 
     // additional helper methods
      
    def logIn(username: String, password: String):Option[LocalUser]={
      val u = selectWithFilter(("USERNAME",username)).headOption
      u.collect(new PartialFunction[LocalUser,LocalUser]{
        def apply(u:LocalUser)=u
        def isDefinedAt(u:LocalUser)=BCrypt.checkpw(password, u.password)})
    }
  
}
