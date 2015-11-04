package controllers.gsn.auth

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import models.gsn.auth.Group
import views.html._
import be.objectify.deadbolt.scala.DeadboltActions
import security.gsn.GSNScalaDeadboltHandler
import javax.inject.Inject
import scala.collection.JavaConverters._
import play.core.j.JavaHelpers
import play.mvc.Http.Context
      
object PermissionsController extends Controller with DeadboltActions {
  
 
  def groups = Restrict(Array(LocalAuthController.USER_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request =>
      //hack to work with java-style templates
      Context.current.set(JavaHelpers.createJavaContext(request))
		  Future(Ok(access.grouplist.render(Group.find.all().asScala, Forms.groupForm)))
      }
  }
  
  def addgroup = Restrict(Array(LocalAuthController.USER_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
     Context.current.set(JavaHelpers.createJavaContext(request))
     Forms.groupForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(access.grouplist.render(Group.find.all().asScala,formWithErrors))
      },
      data => {
        val newGroup = new Group()
        newGroup.name = data.name
        newGroup.description = data.description
        newGroup.save
        Ok(access.grouplist.render(Group.find.all().asScala, Forms.groupForm))
      }
    )
      
		  
        }
      }
  }

}

