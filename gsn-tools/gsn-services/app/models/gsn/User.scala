package models.gsn

import com.feth.play.module.pa.user.AuthUser
import com.feth.play.module.pa.user.AuthUserIdentity
import gsn.data.DataStore
import org.mindrot.jbcrypt.BCrypt
import java.sql.ResultSet
import scala.util.Try
import controllers.gsn.Global

case class User(_pk: Int, id: String, provider: String) extends Model(_pk)

object UserManager extends AbstractManager[User] {
  
  
  def getTableName: String = "AUTH_USER"
  
  def buildFromResultSet(rs: ResultSet): Try[User] =
      Try(User(rs.getInt(0),rs.getString(1),rs.getString(2)))
  
  def stringForInsert(u: User): String = s"""'${u.id}','${u.provider}'"""
    
  def stringForUpdate(u: User): String = s"""ID='${u.id}',PROVIDER='${u.provider}'"""
  
  // additional helper methods
  
  def getUser(id: String): User = {
      val q=s"""SELECT * FROM AUTH_USER WHERE ID='$id'"""
      Global.ds.withSession { implicit session => 
          val rs=session.conn.createStatement.executeQuery(q)
          if (rs.next){
              User(rs.getInt(0),rs.getString(1), rs.getString(2))
          }else{
              null
          }
      }
  }
  
  def createUserIfNotExists(authUser: AuthUser): String = {
    if (getUser(authUser.getId) == null){
      updateOrCreate(User(-1,authUser.getId,authUser.getProvider))
    }
    authUser.getId
  }
  
  def findUserByAuthUserIdentity(identity: AuthUserIdentity): String = {
    val u = getUser(identity.getId)
    if (u != null) u.id else null
  }
  
  def addLinkedAccount(oldUser: AuthUser, newUser: AuthUser): AuthUser = {
    //not supported yet
    null
  }
  
  def merge(oldUser: AuthUser, newUser: AuthUser): AuthUser = {
    //not supported yet
    null
  }
  
}