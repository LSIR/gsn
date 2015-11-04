package controllers.gsn.auth

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import play.data.validation.Constraints.Required;

case class GSNGroup(description: String, name: String) {


	}

object Forms {
 val groupForm = Form(
  mapping(
    "name" -> nonEmptyText,
    "description" -> text
  )(GSNGroup.apply)(GSNGroup.unapply)
)
}