package controllers.gsn.api

import play.api.mvc._

object GsnProxy extends Controller {  

    def v1(path: String) = Action {request=>
    val qString: String = if(request.queryString.nonEmpty) "?" + request.queryString else ""
         TemporaryRedirect("/old/" + path + qString )
    }
}
