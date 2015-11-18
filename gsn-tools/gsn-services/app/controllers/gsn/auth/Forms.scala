package controllers.gsn.auth

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.data.validation.Constraints.Required;

case class GSNGroup(description: String, name: String) {}
case class GSNClient(response_type: String, client_secret: String, client_id: String) {}
case class GSNEditClient(client_secret: String, client_id: String, name: String, redirect: String, action: String, id: Long)

object Forms {
 val groupForm = Form(
  mapping(
    "name" -> nonEmptyText,
    "description" -> text
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
    "id" -> longNumber
  )(GSNEditClient.apply)(GSNEditClient.unapply)
)
}
