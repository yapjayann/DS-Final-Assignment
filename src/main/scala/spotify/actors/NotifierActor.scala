package actors

import akka.actor.{Actor, ActorRef}

class NotifierActor extends Actor {
  private var subscribers: Set[ActorRef] = Set.empty

  override def receive: Receive = {
    case message: String =>
      subscribers.foreach(_ ! message)

    case subscriber: ActorRef =>
      subscribers += subscriber
  }
}
