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
* File: app/controllers/gsn/auth/Forms.scala
*
* @author Julien Eberle
*
*/
package controllers.gsn.auth

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.data.validation.Constraints.Required;

case class GSNGroup(name: String, description: String, action: String, id: Long) {}
case class GSNClient(response_type: String, client_secret: String, client_id: String) {}
case class GSNEditClient(name: String, client_secret: String, client_id: String,  redirect: String, action: String, id: Long, linked: Boolean)

object Forms {
 val groupForm = Form(
  mapping(
    "name" -> nonEmptyText,
    "description" -> text,
    "action" -> nonEmptyText,
    "id" -> longNumber
  )(GSNGroup.apply)(GSNGroup.unapply)
)
 val clientForm = Form(
  mapping(
    "response_type" -> nonEmptyText,
    "client_secret" -> nonEmptyText,
    "client_id" -> nonEmptyText
  )(GSNClient.apply)(GSNClient.unapply)
)
 val editClientForm = Form(
     mapping(
    "name" -> nonEmptyText,
    "client_secret" -> nonEmptyText,
    "client_id" -> nonEmptyText,
    "redirect" -> nonEmptyText,
    "action" -> nonEmptyText,
    "id" -> longNumber,
    "linked" -> boolean
    
  )(GSNEditClient.apply)(GSNEditClient.unapply)
)
}
