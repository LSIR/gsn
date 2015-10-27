package controllers.gsn

import gsn.security._

import scalaoauth2.provider._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.Date
import models.gsn._

class GSNDataHandler extends DataHandler[LocalUser] {

  
  def validateClient(clientCredential: ClientCredential, grantType: String): Future[Boolean] =  Future {
    //always require client credentials
    ApplicationManager.logIn(clientCredential.clientId, clientCredential.clientSecret)
  }

  def findUser(username: String, password: String): Future[Option[LocalUser]] = Future {
    LocalUserManager.logIn(username, password)
  }

  def createAccessToken(authInfo: AuthInfo[LocalUser]): Future[AccessToken] = Future {
    val c = ApplicationManager.selectWithFilter(("ID",authInfo.clientId.getOrElse(""))).headOption
    val t = TokenManager.generateNew(Some(authInfo.user._pk),c.map(x=>x._pk))
    AccessToken(t.token, t.refresh, Some("all"), Some(t.duration/1000),new Date(t.creation))
  }
  
  def getAllTokens(authInfo: AuthInfo[LocalUser]): List[Token] = {
    val c = ApplicationManager.selectWithFilter(("ID",authInfo.clientId.getOrElse(""))).headOption
    var l = List[(String,Any)](("USER",authInfo.user._pk))
    if (c.isDefined) l = l.::(("APP",c.get._pk))
    TokenManager.selectWithFilter(l:_*)
  }

  def getStoredAccessToken(authInfo: AuthInfo[LocalUser]): Future[Option[AccessToken]] = Future {
    getAllTokens(authInfo).map(t => AccessToken(t.token, t.refresh, Some("all"), Some(t.duration/1000),new Date(t.creation))).headOption
  }

  def refreshAccessToken(authInfo: AuthInfo[LocalUser], refreshToken: String): Future[AccessToken] = {
    // refreshToken already validated
    //clean tokens
    getAllTokens(authInfo).map { x => TokenManager.deleteByPk(x._pk) }
    //get a new one
    createAccessToken(authInfo: AuthInfo[LocalUser])
  }
  
  def appToAuthInfo(app:Option[Application]):Option[AuthInfo[LocalUser]] = 
    app.map(x=>LocalUserManager.getByPk(x.user)).flatten.map(x=>AuthInfo[LocalUser](x,app.map(y=>y.id),Some("all"),app.map(y=>y.redirect).flatten))

  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[LocalUser]]] = Future {
      appToAuthInfo(ApplicationManager.selectWithFilter(("CODE",code)).headOption)
  }

  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[LocalUser]]] = Future {
    appToAuthInfo(TokenManager.selectWithFilter(("REFRESH",refreshToken)).headOption.map(x=>ApplicationManager.getByPk(x._pk)).flatten)
  }

  def findClientUser(clientCredential: ClientCredential, scope: Option[String]): Future[Option[LocalUser]] = Future {
    ApplicationManager.selectWithFilter(("ID",clientCredential.clientId)).headOption.map(x=>LocalUserManager.getByPk(x.user)).flatten
  }

  def deleteAuthCode(code: String): Future[Unit] = Future{
    val app = ApplicationManager.selectWithFilter(("CODE",code)).headOption
    app.map(x => models.gsn.Application(x._pk,x.name,x.id,x.secret,x.user,None,x.redirect) ).map(x=>ApplicationManager.updateOrCreate(x))
  }

  def findAccessToken(token: String): Future[Option[AccessToken]] = Future {
        TokenManager.selectWithFilter(("TOKEN",token)).headOption.map(t => AccessToken(t.token, t.refresh, Some("all"), Some(t.duration/1000),new Date(t.creation)))
  }

  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[LocalUser]]] = Future {
    appToAuthInfo(TokenManager.selectWithFilter(("TOKEN",accessToken.token)).headOption.map(x=>ApplicationManager.getByPk(x._pk)).flatten)
  }

}