package models.gsn

import java.sql.ResultSet
import scala.util.Try
import scala.util.Random


case class Token(_pk: Int, user: Option[Int], app: Option[Int], token: String, refresh: Option[String], creation: Long, duration: Long) extends Model(_pk)

object TokenManager extends AbstractManager[Token] {
  
  override def getTableName: String = "AC_OAUTH_TOKEN"
  
  override def buildFromResultSet(rs: ResultSet): Try[Token] = 
    Try(Token(rs.getInt(0),Option(rs.getInt(1)), Option(rs.getInt(2)), rs.getString(3), Option(rs.getString(4)), rs.getLong(5), rs.getLong(6)))
  
  override def stringForInsert(token: Token): String = 
    s"""'${token.user.getOrElse("NULL")}','${token.app.getOrElse("NULL")}','${token.token}','${token.refresh.getOrElse("NULL")}','${token.creation}','${token.duration}'"""
    
  override def stringForUpdate(token: Token): String = 
    s"""USER='${token.user.getOrElse("NULL")}',APP='${token.app.getOrElse("NULL")}',TOKEN='${token.token}',REFRESH='${token.refresh.getOrElse("NULL")}',CREATION='${token.creation}',DURATION='${token.duration}'"""
     
  // additional helper methods
    
   def generateNew(user: Option[Int], app: Option[Int]): Token = {
     val r = new Random()
     val tkn = r.alphanumeric.take(32).mkString("")
     updateOrCreate(Token(-1,user,app,tkn,Some(r.alphanumeric.take(32).mkString("")),System.currentTimeMillis(),3600000L))
     selectWithFilter(("TOKEN",tkn)).head
   }
}