package spotify.actors

import akka.actor.{Actor, Props}
import spotify.models.User
import spotify.actors.Messages._

class UserManagerActor extends Actor {
  private var users: Map[String, User] = Map.empty // Registered users

  override def receive: Receive = {
    case LoginUser(user, _) =>
      context.system.log.info(s"Login attempt for user: ${user.username}")

      if (users.contains(user.username)) {
        context.system.log.info(s"Login failed for user: ${user.username} (already exists).")
        sender() ! false // Reply directly to the original sender
      } else {
        users += (user.username -> user)
        context.system.log.info(s"User '${user.username}' logged in successfully.")
        sender() ! true // Reply directly to the original sender
      }

    case LogoutUser(username, _) =>
      context.system.log.info(s"Logout attempt for user: $username")
      if (users.contains(username)) {
        users -= username
        sender() ! true
        context.system.log.info(s"User '$username' logged out successfully.")
      } else {
        sender() ! false
        context.system.log.info(s"Logout failed for user: $username (not found).")
      }

    case _ =>
      context.system.log.warning("Received an unknown message.")
  }
}

object UserManagerActor {
  def props(): Props = Props[UserManagerActor]
}
