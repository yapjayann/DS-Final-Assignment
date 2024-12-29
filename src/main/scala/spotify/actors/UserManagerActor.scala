package actors

import akka.actor.{Actor, Props}
import actors.Messages._

class UserManagerActor extends Actor {
  private var users: Set[String] = Set.empty // Set to track registered users

  override def receive: Receive = {
    case LoginUser(username) =>
      if (username.isEmpty) {
        println("LoginUser failed: Username cannot be empty.")
        sender() ! false
      } else if (users.contains(username)) {
        println(s"LoginUser failed: Username '$username' already exists.")
        sender() ! false
      } else {
        users += username
        println(s"LoginUser successful: Username '$username' registered.")
        sender() ! true
      }

    case LogoutUser(username) =>
      if (users.contains(username)) {
        users -= username
        println(s"LogoutUser successful: Username '$username' removed.")
        sender() ! true
      } else {
        println(s"LogoutUser failed: Username '$username' not found.")
        sender() ! false
      }

    case AddContributor(username) =>
      val exists = users.contains(username)
      if (exists) {
        println(s"AddContributor successful: Username '$username' is a registered user.")
      } else {
        println(s"AddContributor failed: Username '$username' is not a registered user.")
      }
      sender() ! exists

    case _ =>
      println("Received unknown message in UserManagerActor.")
  }
}

object UserManagerActor {
  def props(): Props = Props[UserManagerActor]
}
