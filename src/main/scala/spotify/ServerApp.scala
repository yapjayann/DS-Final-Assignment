package spotify

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import spotify.actors.{PlaylistActor, UserManagerActor, SongDatabaseActor, Messages} // Import Messages explicitly
import spotify.UpnpManager // Fully qualify the reference to UpnpManager

import scala.concurrent.ExecutionContext

object ServerApp {
  private var actorSystem: Option[ActorSystem] = None

  def start(): ActorSystem = {
    val config = ConfigFactory.load()
    val system = ActorSystem("SpotifySystem", config)
    implicit val ec: ExecutionContext = system.dispatcher // Implicit ExecutionContext

    // Initialize actors
    val playlistActor = system.actorOf(Props[PlaylistActor], "PlaylistActor")
    system.log.info(s"PlaylistActor initialized at: ${playlistActor.path}")

    val userManagerActor = system.actorOf(Props[UserManagerActor], "UserManagerActor")
    system.log.info(s"UserManagerActor initialized at: ${userManagerActor.path}")

    val songDatabaseActor = system.actorOf(Props[SongDatabaseActor], "SongDatabaseActor")
    system.log.info(s"SongDatabaseActor initialized at: ${songDatabaseActor.path}")

    // Debugging: Add ping support to verify actor communication
    playlistActor ! "ping"
    songDatabaseActor ! "ping"

    // UPnP Initialization
    val upnpManager = system.actorOf(Props[spotify.UpnpManager], "UpnpManager")
    upnpManager ! UpnpManager.AddPortMapping(config.getInt("akka.remote.artery.canonical.port"))

    system.log.info("Spotify Server is up and running!")
    actorSystem = Some(system)
    system
  }

  def stop(): Unit = {
    actorSystem.foreach { system =>
      implicit val ec: ExecutionContext = system.dispatcher
      system.terminate()
      system.whenTerminated.foreach(_ => println("Spotify Server has stopped."))
    }
  }

  def main(args: Array[String]): Unit = {
    println("Starting Spotify Server...")
    start()

    sys.addShutdownHook {
      println("Shutting down Spotify Server...")
      stop()
    }
  }
}
