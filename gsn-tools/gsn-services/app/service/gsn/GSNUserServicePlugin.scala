package service.gsn

import com.feth.play.module.pa.service.UserServicePlugin
import com.feth.play.module.pa.user.AuthUser
import com.feth.play.module.pa.user.AuthUserIdentity
import play.Application

import controllers.gsn.Global
import models.gsn.UserManager

class GSNUserServicePlugin (a: Application) extends UserServicePlugin (a ){
    
    override def save(authUser: AuthUser): Object = {
        UserManager.createUserIfNotExists(authUser)
    }
    
    override def getLocalIdentity(identity: AuthUserIdentity): Object = {
        // For production: Caching might be a good idea here...
        // ...and dont forget to sync the cache when users get deactivated/deleted
        UserManager.findUserByAuthUserIdentity(identity)
    }

    override def merge(newUser: AuthUser, oldUser: AuthUser): AuthUser = {
        if (!oldUser.equals(newUser)) {
            UserManager.merge(oldUser, newUser)
        }
        oldUser
    }

    override def link(oldUser: AuthUser, newUser: AuthUser): AuthUser = {
        UserManager.addLinkedAccount(oldUser, newUser)
        null
    }

  
}