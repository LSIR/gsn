package controllers.gsn.auth

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.data.validation.Constraints.Required;

case class GSNGroup(description: String, name: String) {


	}
case class GSNClient(response_type: String, redirect_uri: String, client_id: String) {


	}

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
    "redirect_uri" -> nonEmptyText,
    "client_id" -> nonEmptyText
  )(GSNClient.apply)(GSNClient.unapply)
)
}