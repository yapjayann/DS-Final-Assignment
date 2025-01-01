package spotify.actors

import akka.actor.{Actor, Props, ActorRef}
import spotify.models.User
import spotify.actors.Messages._

class UserManagerActor extends Actor {
  private var users: Map[String, User] = Map.empty

  override def receive: Receive = {
    case LoginUser(user, replyTo) =>
      if (users.contains(user.username)) {
        replyTo ! false
        context.system.log.info(s"Login failed for user: ${user.username} (already exists).")
      } else {
        users += (user.username -> user)
        replyTo ! true
        context.system.log.info(s"User '${user.username}' logged in successfully.")
      }

    case LogoutUser(username, replyTo) =>
      if (users.contains(username)) {
        users -= username
        replyTo ! true
        context.system.log.info(s"User '$username' logged out successfully.")
      } else {
        replyTo ! false
        context.system.log.info(s"Logout failed for user: $username (not found).")
      }

    case AddContributor(username, playlistId, replyTo) =>
      if (users.contains(username)) {
        // Log contributor addition with playlistId context
        context.system.log.info(s"Adding contributor '$username' to playlist '$playlistId'.")
        replyTo ! true
        context.system.log.info(s"Contributor '$username' added successfully to playlist '$playlistId'.")
      } else {
        replyTo ! false
        context.system.log.info(s"Failed to add contributor: '$username' (user not found).")
      }

    case _ =>
      context.system.log.warning("Received an unknown message.")
      sender() ! "Unknown message type."
  }
}

object UserManagerActor {
  def props(): Props = Props[UserManagerActor]
}
