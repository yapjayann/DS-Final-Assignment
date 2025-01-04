package spotify

import akka.actor.typed.{ActorSystem, ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.receptionist.{ServiceKey, Receptionist}
import akka.util.Timeout
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import com.typesafe.config.ConfigFactory
import spotify.models.{Playlist, User}
import spotify.controllers.{LoginController, MainPageController, PlaylistController}
import spotify.actors.{UserManagerActor, PlaylistActor, SongDatabaseActor}
import spotify.actors.Messages.Command

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Await}

object MainApp extends App {
  // Define ServiceKeys for each actor
  val UserManagerKey = ServiceKey[Command]("userManagerActor")
  val PlaylistKey = ServiceKey[Command]("playlistActor")
  val SongDatabaseKey = ServiceKey[Command]("songDatabaseActor")

  // Configuration for Akka Actor System
  val config = ConfigFactory.load("application.conf")

  // Initialize the Actor System with a root behavior
  val actorSystem: ActorSystem[Command] = ActorSystem(
    Behaviors.setup[Command] { context =>
      // Create the actor instances
      val userManagerActor = context.spawn(UserManagerActor(), "UserManagerActor")
      val playlistActor = context.spawn(PlaylistActor(), "PlaylistActor")
      val songDatabaseActor = context.spawn(SongDatabaseActor(), "SongDatabaseActor")

      // Register actors with the receptionist
      context.system.receptionist ! Receptionist.Register(UserManagerKey, userManagerActor)
      context.system.receptionist ! Receptionist.Register(PlaylistKey, playlistActor)
      context.system.receptionist ! Receptionist.Register(SongDatabaseKey, songDatabaseActor)

      // Watch the actors
      context.watch(userManagerActor)
      context.watch(playlistActor)
      context.watch(songDatabaseActor)

      Behaviors.empty
    },
    "SpotifySystem",
    config
  )

  // Actor references
  @volatile private var userManagerActorRef: ActorRef[Command] = _
  @volatile private var playlistActorRef: ActorRef[Command] = _
  @volatile private var songDatabaseActorRef: ActorRef[Command] = _

  // Public accessor methods
  def userManagerActor: ActorRef[Command] = userManagerActorRef
  def playlistActor: ActorRef[Command] = playlistActorRef
  def songDatabaseActor: ActorRef[Command] = songDatabaseActorRef

  // Setup actor references
  def setupActorRefs(): Future[Unit] = {
    implicit val timeout: Timeout = 5.seconds

    // Create a behavior for the lookup actor
    val lookupBehavior = Behaviors.setup[Receptionist.Listing] { context =>
      // Subscribe to all services
      context.system.receptionist ! Receptionist.Subscribe(UserManagerKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(PlaylistKey, context.self)
      context.system.receptionist ! Receptionist.Subscribe(SongDatabaseKey, context.self)

      Behaviors.receiveMessage {
        case UserManagerKey.Listing(listings) =>
          listings.headOption.foreach(ref => userManagerActorRef = ref)
          Behaviors.same
        case PlaylistKey.Listing(listings) =>
          listings.headOption.foreach(ref => playlistActorRef = ref)
          Behaviors.same
        case SongDatabaseKey.Listing(listings) =>
          listings.headOption.foreach(ref => songDatabaseActorRef = ref)
          Behaviors.same
      }
    }

    // Spawn the lookup actor
    val lookupActor = actorSystem.systemActorOf(lookupBehavior, "ServiceLookupActor")

    // Return a Future that completes when all references are set
    Future {
      while (userManagerActorRef == null || playlistActorRef == null || songDatabaseActorRef == null) {
        Thread.sleep(100)
      }
    }
  }

  // Initialize everything before launching JavaFX
  Await.result(setupActorRefs(), 10.seconds)

  // Launch JavaFX application
  Application.launch(classOf[MainApp], args: _*)
}

class MainApp extends Application {
  private var primaryStage: Stage = _
  implicit val timeout: Timeout = 5.seconds

  override def start(primaryStage: Stage): Unit = {
    this.primaryStage = primaryStage
    this.primaryStage.setTitle("Spotify Clone")
    showLoginScreen()
  }

  private def showLoginScreen(): Unit = {
    try {
      val loader = new FXMLLoader(getClass.getResource("/com/spotify/view/Login.fxml"))
      val root: AnchorPane = loader.load()
      val scene = new Scene(root)

      val controller = loader.getController[LoginController]
      controller.setApp(this)

      primaryStage.setScene(scene)
      primaryStage.show()
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new RuntimeException("Failed to load Login.fxml", e)
    }
  }

  def showMainPage(user: User): Unit = {
    try {
      val loader = new FXMLLoader(getClass.getResource("/com/spotify/view/MainPage.fxml"))
      val root: AnchorPane = loader.load()
      val scene = new Scene(root)

      val controller = loader.getController[MainPageController]
      controller.setCurrentUser(user)
      controller.setMainApp(this)

      primaryStage.setScene(scene)
      primaryStage.show()
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new RuntimeException("Failed to load MainPage.fxml", e)
    }
  }

  def showPlaylistScreen(user: User, playlist: Playlist): Unit = {
    try {
      val loader = new FXMLLoader(getClass.getResource("/com/spotify/view/Playlist.fxml"))
      val root: AnchorPane = loader.load()
      val scene = new Scene(root)

      val controller = loader.getController[PlaylistController]
      controller.setCurrentUserAndPlaylist(user, playlist)
      controller.setMainApp(this)

      primaryStage.setScene(scene)
      primaryStage.show()
    } catch {
      case e: Exception =>
        e.printStackTrace()
        throw new RuntimeException("Failed to load Playlist.fxml", e)
    }
  }

  override def stop(): Unit = {
    MainApp.actorSystem.terminate()
  }
}