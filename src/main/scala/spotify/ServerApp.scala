package spotify

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{ServiceKey, Receptionist}
import com.typesafe.config.ConfigFactory
import spotify.actors.{PlaylistActor, UserManagerActor, SongDatabaseActor}
import spotify.actors.Messages.Command

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ServerApp {
  // Define ServiceKeys for each actor
  val UserManagerKey = ServiceKey[Command]("userManagerActor")
  val PlaylistKey = ServiceKey[Command]("playlistActor")
  val SongDatabaseKey = ServiceKey[Command]("songDatabaseActor")

  private var actorSystem: Option[ActorSystem[Command]] = None

  def start(): ActorSystem[Command] = {
    val config = ConfigFactory.load("application.conf")

    val rootBehavior: Behavior[Command] = Behaviors.setup { context =>
      // Create the actor instances
      val playlistActor = context.spawn(PlaylistActor(), "PlaylistActor")
      val userManagerActor = context.spawn(UserManagerActor(), "UserManagerActor")
      val songDatabaseActor = context.spawn(SongDatabaseActor(), "SongDatabaseActor")

      // Register actors with the receptionist using proper ServiceKeys
      context.system.receptionist ! Receptionist.Register(PlaylistKey, playlistActor)
      context.system.receptionist ! Receptionist.Register(UserManagerKey, userManagerActor)
      context.system.receptionist ! Receptionist.Register(SongDatabaseKey, songDatabaseActor)

      context.log.info("All actors initialized and registered")
      Behaviors.empty
    }

    val system = ActorSystem[Command](rootBehavior, "SpotifySystem", config)
    actorSystem = Some(system)

    // Log successful startup
    system.log.info("Spotify Server is up and running!")
    system
  }

  def stop(): Unit = {
    actorSystem.foreach { system =>
      system.terminate()
      // Using global ExecutionContext for whenTerminated
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