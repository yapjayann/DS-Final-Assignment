package spotify

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import actors.{PlaylistActor, UserManagerActor}
import spotify.UpnpManager

import scala.concurrent.ExecutionContext

object ServerApp {
  private var actorSystem: Option[ActorSystem] = None

  def start(): ActorSystem = {
    val config = ConfigFactory.load()
    val system = ActorSystem("SpotifySystem", config)
    implicit val ec: ExecutionContext = system.dispatcher // Implicit ExecutionContext

    // Initialize actors
    system.actorOf(Props[PlaylistActor], "PlaylistActor")
    system.actorOf(Props[UserManagerActor], "UserManagerActor")

    // UPnP Initialization
    val upnpManager = system.actorOf(Props[UpnpManager], "UpnpManager")
    upnpManager ! UpnpManager.AddPortMapping(config.getInt("akka.remote.artery.canonical.port"))

    system.log.info("Spotify Server is up and running!")
    actorSystem = Some(system)
    system
  }

  def stop(): Unit = {
    actorSystem.foreach { system =>
      implicit val ec: ExecutionContext = system.dispatcher // Implicit ExecutionContext
      system.terminate()
      system.whenTerminated.foreach(_ => println("Spotify Server has stopped."))
    }
  }

  // Main method for running the server
  def main(args: Array[String]): Unit = {
    println("Starting Spotify Server...")
    start()

    // Hook for graceful shutdown
    sys.addShutdownHook {
      println("Shutting down Spotify Server...")
      stop()
    }
  }
}





