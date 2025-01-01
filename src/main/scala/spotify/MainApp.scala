package spotify

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.AnchorPane
import javafx.stage.Stage
import akka.actor.{ActorSelection, ActorSystem}
import com.typesafe.config.ConfigFactory
import spotify.models.{Playlist, User}
import controllers.{LoginController, MainPageController, PlaylistController}

import scala.concurrent.ExecutionContext

object MainApp {
  // Load client-specific Akka configuration
  val config = ConfigFactory.load("client.conf")
  val actorSystem: ActorSystem = ActorSystem("SpotifySystem", config)

  // Remote actor references
  val playlistActor: ActorSelection = actorSystem.actorSelection("akka://SpotifySystem@<server-ip>:<port>/user/PlaylistActor")
  val userManagerActor: ActorSelection = actorSystem.actorSelection("akka://SpotifySystem@<server-ip>:<port>/user/UserManagerActor")
}

class MainApp extends Application {
  private var primaryStage: Stage = _

  override def start(primaryStage: Stage): Unit = {
    // Start the server for development/testing
    println("Starting Spotify Server...")
    ServerApp.start()

    this.primaryStage = primaryStage
    this.primaryStage.setTitle("Spotify Clone")

    // Start with the Login Screen
    showLoginScreen()
  }

  // Display the Login Screen
  private def showLoginScreen(): Unit = {
    try {
      println("Loading Login screen...")
      val loader = new FXMLLoader(getClass.getResource("/com/spotify/view/Login.fxml"))
      val root: AnchorPane = loader.load()
      val scene = new Scene(root)

      // Set controller logic
      val controller = loader.getController[LoginController]
      if (controller == null) throw new NullPointerException("LoginController is null!")
      controller.setApp(this)

      primaryStage.setScene(scene)
      primaryStage.show()
      println("Login screen loaded successfully.")
    } catch {
      case e: Exception =>
        println("Error loading Login screen:")
        e.printStackTrace()
    }
  }

  // Display the Main Page Screen
  def showMainPage(user: User): Unit = {
    try {
      println(s"Transitioning to MainPage with user: ${user.username}...")
      val loader = new FXMLLoader(getClass.getResource("/com/spotify/view/MainPage.fxml"))
      val root: AnchorPane = loader.load()
      val scene = new Scene(root)

      // Correctly retrieve the MainPageController
      val controller = loader.getController[MainPageController]
      if (controller == null) throw new NullPointerException("MainPageController is null!")
      controller.setCurrentUser(user)
      controller.setMainApp(this) // Set MainApp reference to MainPageController

      primaryStage.setScene(scene)
      primaryStage.show()
      println("MainPage loaded successfully.")
    } catch {
      case e: Exception =>
        println("Error transitioning to MainPage:")
        e.printStackTrace()
    }
  }

  // Display the Playlist Screen
  def showPlaylistScreen(user: User, playlist: Playlist): Unit = {
    try {
      println(s"Transitioning to Playlist screen with user: ${user.username} and playlist: ${playlist.name}...")
      val loader = new FXMLLoader(getClass.getResource("/com/spotify/view/Playlist.fxml"))
      val root: AnchorPane = loader.load()
      val scene = new Scene(root)

      // Retrieve the PlaylistController
      val controller = loader.getController[PlaylistController]
      if (controller == null) throw new NullPointerException("PlaylistController is null!")
      controller.setCurrentUserAndPlaylist(user, playlist)
      controller.setMainApp(this)
      primaryStage.setScene(scene)
      primaryStage.show()
      println("Playlist screen loaded successfully.")
    } catch {
      case e: Exception =>
        println("Error transitioning to Playlist screen:")
        e.printStackTrace()
    }
  }

  override def stop(): Unit = {
    try {
      println("Shutting down application...")

      // Stop the server
      ServerApp.stop()

      // Terminate the actor system
      implicit val ec: ExecutionContext = MainApp.actorSystem.dispatcher
      MainApp.actorSystem.terminate()
      MainApp.actorSystem.whenTerminated.foreach(_ => println("Akka actor system terminated successfully."))
    } catch {
      case e: Exception =>
        println("Error during application shutdown:")
        e.printStackTrace()
    }
    super.stop()
  }
}

object SpotifyApp {
  def main(args: Array[String]): Unit = {
    Application.launch(classOf[MainApp], args: _*)
  }
}

