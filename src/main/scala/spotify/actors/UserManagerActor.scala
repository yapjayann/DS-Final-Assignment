package spotify.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import spotify.models.User
import spotify.actors.Messages._

object UserManagerActor {
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    var users: Map[String, User] = Map.empty

    Behaviors.receiveMessage {
      case LoginUser(user, replyTo) =>
        if (users.contains(user.username)) {
          context.log.info(s"Login failed: User '${user.username}' already exists")
          replyTo ! false
        } else {
          users += (user.username -> user)
          context.log.info(s"User '${user.username}' logged in successfully")
          replyTo ! true
        }
        Behaviors.same

      case LogoutUser(username, replyTo) =>
        if (users.contains(username)) {
          users -= username
          context.log.info(s"User '$username' logged out successfully")
          replyTo ! true
        } else {
          context.log.info(s"Logout failed: User '$username' not found")
          replyTo ! false
        }
        Behaviors.same

      case GetAllUsers(replyTo) =>
        context.log.info(s"Retrieved ${users.size} users")
        replyTo ! users.values.toList
        Behaviors.same

      case msg =>
        context.log.info(s"Received unknown message: $msg")
        Behaviors.same
    }
  }
}