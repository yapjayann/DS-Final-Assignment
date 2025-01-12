package spotify

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{ServiceKey, Receptionist}
import com.typesafe.config.ConfigFactory
import spotify.actors.{PlaylistActor, UserManagerActor, SongDatabaseActor}
import spotify.actors.Messages.Command

import scala.concurrent.ExecutionContext

object ServerApp {
  // Define ServiceKeys for actors
  val UserManagerKey = ServiceKey[Command]("userManagerActor")
  val PlaylistKey = ServiceKey[Command]("playlistActor")
  val SongDatabaseKey = ServiceKey[Command]("songDatabaseActor")

  def main(args: Array[String]): Unit = {
    // Define an implicit ExecutionContext
    implicit val ec: ExecutionContext = ExecutionContext.global

    val config = ConfigFactory.load("application.conf")

    // Start the server ActorSystem
    val system: ActorSystem[Command] = ActorSystem(
      Behaviors.setup[Command] { context =>
        try {
          // Spawn actors
          val playlistActor = context.spawn(PlaylistActor(), "PlaylistActor")
          val userManagerActor = context.spawn(UserManagerActor(), "UserManagerActor")
          val songDatabaseActor = context.spawn(SongDatabaseActor(), "SongDatabaseActor")

          // Register actors with the Receptionist
          context.system.receptionist ! Receptionist.Register(PlaylistKey, playlistActor)
          context.system.receptionist ! Receptionist.Register(UserManagerKey, userManagerActor)
          context.system.receptionist ! Receptionist.Register(SongDatabaseKey, songDatabaseActor)

          // Log successful initialization
          context.log.info("Server initialized:")
          context.log.info("- PlaylistActor registered with ServiceKey 'playlistActor'")
          context.log.info("- UserManagerActor registered with ServiceKey 'userManagerActor'")
          context.log.info("- SongDatabaseActor registered with ServiceKey 'songDatabaseActor'")
          println("Spotify Server is running. Press ENTER to terminate.")

          Behaviors.empty
        } catch {
          case ex: Exception =>
            context.log.error(s"Error during server setup: ${ex.getMessage}")
            throw ex
        }
      },
      "SpotifyServer",
      config
    )

    // Wait for termination signal
    try {
      scala.io.StdIn.readLine()
    } finally {
      println("Terminating Spotify Server...")
      system.terminate()
      system.whenTerminated.map(_ => println("Spotify Server stopped."))
    }
  }
}
