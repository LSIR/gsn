package controllers.gsn.auth

import scala.concurrent.{Future, Promise}
import akka.actor._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.mvc._
import models.gsn.auth.{Group, User, DataSource, SecurityRole}
import views.html._
import be.objectify.deadbolt.scala.DeadboltActions
import security.gsn.GSNScalaDeadboltHandler
import javax.inject.Inject
import scala.collection.JavaConverters._
import play.core.j.JavaHelpers
import play.mvc.Http.Context
import play.api.libs.concurrent.Akka
import play.api.Play.current
import gsn.data._
      
object PermissionsController extends Controller with DeadboltActions {
  
 
    def vs = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request =>

        val p=Promise[Seq[SensorData]]     
        val st=Akka.system.actorSelection("/user/gsnSensorStore")
        val q=Akka.system.actorOf(Props(new QueryActor(p)))
        q ! GetAllSensors(false,None)
        p.future.map{data =>  
            Context.current.set(JavaHelpers.createJavaContext(request))
            data.map(s => Option(DataSource.findByValue(s.sensor.name)).getOrElse({
              val d = new DataSource()
              d.value = s.sensor.name
              d.save()
              d
            })
            )
            
  		      Ok(access.vslist.render(DataSource.find.all().asScala, Group.find.all().asScala, User.find.all().asScala))
		   }
    }}
  
  def addgroup = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
     Context.current.set(JavaHelpers.createJavaContext(request))
     var ret:Result = null
     Forms.groupForm.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(access.grouplist.render(Group.find.all().asScala, User.find.all().asScala,formWithErrors))
      },
      data => {
        data.action match {
              case "add" => {
                                val newGroup = new Group()
                                newGroup.name = data.name
                                newGroup.description = data.description
                                newGroup.save
                            }
              case "edit" => {
                                val g = Group.find.byId(data.id)
                                if (g == null){ 
                                  ret = NotFound
                                } else {
                                  g.name = data.name
                                  g.description = data.description
                                  g.update
                                }
                             }
              case "del" => {
                                val g = Group.find.byId(data.id)
                                if (g == null) ret = NotFound
                                else {
                                  g.users.clear               
                                  g.saveManyToManyAssociations("users")
                                  g.delete
                                }
                            }
               }
        
        if (ret != null)  ret else Ok(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm))
      }
    )
      
		  
        }
      }
  }
  
  def addtogroup = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
     Context.current.set(JavaHelpers.createJavaContext(request))
        val g = request.queryString.get("group_id").map { x => Group.find.byId(x.head.toLong) }
        val u = request.queryString.get("user_id").map { x => User.find.byId(x.head.toLong) }

        u.fold(BadRequest(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm)))(user => {
            g.fold(BadRequest(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm)))(group => {
                group.users.add(user)
                group.saveManyToManyAssociations("users")
                Ok(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm))
            })
        })   
        }
      }
  }
  
  def removefromgroup = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
     Context.current.set(JavaHelpers.createJavaContext(request))
        val g = request.queryString.get("group_id").map { x => Group.find.byId(x.head.toLong) }
        val u = request.queryString.get("user_id").map { x => User.find.byId(x.head.toLong) }

        u.fold(BadRequest(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm)))(user => {
            g.fold(BadRequest(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm)))(group => {
                group.users.remove(user)
                group.saveManyToManyAssociations("users")
                Ok(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm))
            })
        })   
        }
      }
  }
  
  def groups = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
        //hack to work with java-style templates
        Context.current.set(JavaHelpers.createJavaContext(request))
        
  		  Ok(access.grouplist.render(Group.find.all().asScala,User.find.all().asScala, Forms.groupForm))
		  }
      }
  }
  
  def users = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
        //hack to work with java-style templates
        Context.current.set(JavaHelpers.createJavaContext(request))
        
  		  Ok(access.userlist.render(User.find.all().asScala, SecurityRole.find.all().asScala))
		  }
      }
  }
  
  def addrole = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
     Context.current.set(JavaHelpers.createJavaContext(request))
        val r = request.queryString.get("role_id").map { x => SecurityRole.find.byId(x.head.toLong) }
        val u = request.queryString.get("user_id").map { x => User.find.byId(x.head.toLong) }

        u.fold(BadRequest(access.userlist.render(User.find.all().asScala, SecurityRole.find.all().asScala)))(user => {
            r.fold(BadRequest(access.userlist.render(User.find.all().asScala, SecurityRole.find.all().asScala)))(role => {
                user.roles.add(role)
                user.saveManyToManyAssociations("roles")
                Ok(access.userlist.render(User.find.all().asScala, SecurityRole.find.all().asScala))
            })
        })   
        }
      }
  }
  
  def removerole = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
     Context.current.set(JavaHelpers.createJavaContext(request))
        val r = request.queryString.get("role_id").map { x => SecurityRole.find.byId(x.head.toLong) }
        val u = request.queryString.get("user_id").map { x => User.find.byId(x.head.toLong) }

        u.fold(BadRequest(access.userlist.render(User.find.all().asScala, SecurityRole.find.all().asScala)))(user => {
            r.fold(BadRequest(access.userlist.render(User.find.all().asScala, SecurityRole.find.all().asScala)))(role => {
                user.roles.remove(role)
                user.saveManyToManyAssociations("roles")
                Ok(access.userlist.render(User.find.all().asScala, SecurityRole.find.all().asScala))
            })
        })   
        }
      }
  }
  
  def addtovs = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
      Context.current.set(JavaHelpers.createJavaContext(request))
      val v = request.queryString.get("vs_id").map { x => DataSource.find.byId(x.head.toLong) }
      v.fold(BadRequest(access.vslist.render(DataSource.find.all().asScala, Group.find.all().asScala, User.find.all().asScala)))(vs => {
          request.queryString.get("id").map {x => x.head match {
              case s if s.startsWith("u") => {
                  vs.users.add(User.find.byId(s.stripPrefix("u").toLong))
                  vs.saveManyToManyAssociations("users")
                  }
              case s if s.startsWith("g") => {
                  vs.groups.add(Group.find.byId(s.stripPrefix("g").toLong))
                  vs.saveManyToManyAssociations("groups")
                  }
          }}
          Ok(access.vslist.render(DataSource.find.all().asScala, Group.find.all().asScala, User.find.all().asScala))
        })   
        }
      }
  }
  
  def removefromvs = Restrict(Array(LocalAuthController.ADMIN_ROLE),new GSNScalaDeadboltHandler) { Action.async { implicit request => Future {
      //hack to work with java-style templates
      Context.current.set(JavaHelpers.createJavaContext(request))
      val v = request.queryString.get("vs_id").map { x => DataSource.find.byId(x.head.toLong) }
      v.fold(BadRequest(access.vslist.render(DataSource.find.all().asScala, Group.find.all().asScala, User.find.all().asScala)))(vs => {
          request.queryString.get("id").map {x => x.head match {
              case s if s.startsWith("u") => {
                  vs.users.remove(User.find.byId(s.stripPrefix("u").toLong))
                  vs.saveManyToManyAssociations("users")
                  }
              case s if s.startsWith("g") => {
                  vs.groups.remove(Group.find.byId(s.stripPrefix("g").toLong))
                  vs.saveManyToManyAssociations("groups")
                  }
          }}
          Ok(access.vslist.render(DataSource.find.all().asScala, Group.find.all().asScala, User.find.all().asScala))
        })   
        }
      }
  }

}

