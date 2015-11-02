package controllers.gsn

import gsn.security._

import scalaoauth2.provider._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Date
import models.gsn.auth._
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser
import providers.gsn.GSNUsernamePasswordAuthUser
import scala.collection.JavaConverters._

class GSNDataHandler extends DataHandler[User] {

  
  def validateClient(clientCredential: ClientCredential, grantType: String): Future[Boolean] =  Future {
    //always require client credentials
    val c = Client.findById(clientCredential.clientId)
    c != null && c.secret == clientCredential.clientSecret
  }

  def findUser(username: String, password: String): Future[Option[User]] = Future {
    Option(User.findByUsernamePasswordIdentity(new GSNUsernamePasswordAuthUser(username,password)))
  }

  def createAccessToken(authInfo: AuthInfo[User]): Future[AccessToken] = Future {
    val c = Client.findById(authInfo.clientId.getOrElse(""))
    val t = OAuthToken.generate(authInfo.user, c)
    AccessToken(t.token, Option(t.refresh), Some("all"), Some(t.duration/1000),new Date(t.creation))
  }
  
  def getAllTokens(authInfo: AuthInfo[User]): List[OAuthToken] = {
    val o = Option(Client.findById(authInfo.clientId.getOrElse("")))
    o.map(x => x.tokens.asScala.toList).getOrElse(List())
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] = Future {
    getAllTokens(authInfo).map(t => AccessToken(t.token, Option(t.refresh), Some("all"), Some(t.duration/1000),new Date(t.creation))).headOption
  }

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] = {
    // refreshToken already validated
    //clean tokens
    getAllTokens(authInfo).map { x => x.delete() }
    //get a new one
    createAccessToken(authInfo: AuthInfo[User])
  }
  
  def appToAuthInfo(app:Option[Client]):Option[AuthInfo[User]] = 
    app.map(x=>x.user).map(x=>AuthInfo[User](x,app.map(y=>y.clientId),Some("all"),app.map(y=>y.redirect)))

  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = Future {
      appToAuthInfo(Option(Client.findByCode(code)))
  }

  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] = Future {
    appToAuthInfo(Option(OAuthToken.findByRefresh(refreshToken)).map(x=>x.client))
  }

  def findClientUser(clientCredential: ClientCredential, scope: Option[String]): Future[Option[User]] = Future {
    Option(Client.findById(clientCredential.clientId)).map(x=>x.user)
  }

  def deleteAuthCode(code: String): Future[Unit] = Future{
    val app = Option(Client.findByCode(code))
    app.map(x => {x.code=null
                  x.save()})
  }

  def findAccessToken(token: String): Future[Option[AccessToken]] = Future {
        Option(OAuthToken.findByToken(token)).map(t => AccessToken(t.token, Option(t.refresh), Some("all"), Some(t.duration/1000),new Date(t.creation)))
  }

  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] = Future {
    appToAuthInfo(Option(OAuthToken.findByToken(accessToken.token)).map(x=>x.client))
  }

}