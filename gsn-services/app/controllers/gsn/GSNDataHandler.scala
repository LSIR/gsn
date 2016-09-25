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
* File: app/controllers/gsn/GSNDataHandler.scala
*
* @author Julien Eberle
*
*/
package controllers.gsn


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
    c != null && c.secret.equals(clientCredential.clientSecret.getOrElse(""))
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
    val c = Client.findById(authInfo.clientId.getOrElse(""))
    OAuthToken.findByUserClient(authInfo.user, c).asScala.toList
  }

  def getStoredAccessToken(authInfo: AuthInfo[User]): Future[Option[AccessToken]] = Future {
    getAllTokens(authInfo).map(t => AccessToken(t.token, Option(t.refresh), Some("all"), Some((t.creation - System.currentTimeMillis + t.duration)/1000),new Date(t.creation))).headOption
  }

  def refreshAccessToken(authInfo: AuthInfo[User], refreshToken: String): Future[AccessToken] = {
    // refreshToken already validated
    //clean tokens
    getAllTokens(authInfo).map { x => x.delete() }
    //get a new one
    createAccessToken(authInfo: AuthInfo[User])
  }
  
  def findAuthInfoByCode(code: String): Future[Option[AuthInfo[User]]] = Future {
      Option(OAuthCode.findByCode(code)).map(c => AuthInfo[User](c.user,Option(c.getClient.getClientId),Some("all"),Option(c.getClient.getRedirect)))
  }

  def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[User]]] = Future {
    Option(OAuthToken.findByRefresh(refreshToken)).map(t => AuthInfo[User](t.user,Option(t.getClient.getClientId),Some("all"),Option(t.getClient.getRedirect)))
  }

  def findClientUser(clientCredential: ClientCredential, scope: Option[String]): Future[Option[User]] = Future {
    Option(Client.findById(clientCredential.clientId)).flatMap(c => if (c.isLinked) Some(c.user) else None)
  }

  def deleteAuthCode(code: String): Future[Unit] = Future{
    Option(OAuthCode.findByCode(code)).map(x => {x.delete()})
  }

  def findAccessToken(token: String): Future[Option[AccessToken]] = Future {
        Option(OAuthToken.findByToken(token)).map(t => AccessToken(t.token, Option(t.refresh), Some("all"), Some(t.duration/1000),new Date(t.creation)))
  }

  def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[User]]] = Future {
    Option(OAuthToken.findByToken(accessToken.token)).map(t => AuthInfo[User](t.user,Option(t.getClient.getClientId),Some("all"),Option(t.getClient.getRedirect)))
  }

}