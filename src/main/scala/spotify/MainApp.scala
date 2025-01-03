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
import actors.UserManagerActor

object MainApp {
  val config = ConfigFactory.load("client.conf")
  val actorSystem: ActorSystem = ActorSystem("SpotifySystem", config)

  // Remote actor references
  val playlistActor: ActorSelection = actorSystem.actorSelection("akka://SpotifySystem/user/PlaylistActor")
  val userManagerActor: ActorSelection = actorSystem.actorSelection("akka://SpotifySystem/user/UserManagerActor")

  // Initialize UserManagerActor locally
  actorSystem.actorOf(UserManagerActor.props(), "UserManagerActor")
}

class MainApp extends Application {
  private var primaryStage: Stage = _

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
      case e: Exception => e.printStackTrace()
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
      case e: Exception => e.printStackTrace()
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
      case e: Exception => e.printStackTrace()
    }
  }

  override def stop(): Unit = {
    try {
      MainApp.actorSystem.terminate()
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
}
