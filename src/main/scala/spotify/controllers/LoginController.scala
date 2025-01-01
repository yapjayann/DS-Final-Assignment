package controllers

import spotify.MainApp
import javafx.fxml.FXML
import javafx.scene.control.{Button, Label, ProgressIndicator, TextField}
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.ActorRef
import akka.actor.TypedActor.self
import javafx.application.Platform

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import spotify.actors.Messages
import spotify.models.User

class LoginController {

  // FXML references
  @FXML private var usernameField: TextField = _
  @FXML private var errorLabel: Label = _
  @FXML private var loadingIndicator: ProgressIndicator = _
  @FXML private var loginButton: Button = _

  // Akka Actor System
  private val system: ActorSystem = MainApp.actorSystem
  private var userActor: ActorRef = system.actorOf(spotify.actors.UserManagerActor.props(), "UserManagerActor")

  // Reference to MainApp for screen transitions
  private var mainApp: MainApp = _

  // Method to set the MainApp instance
  def setApp(app: MainApp): Unit = {
    this.mainApp = app
    println(s"MainApp reference set in LoginController: $mainApp")
  }

  // Handle login button action
  @FXML
  private def handleLogin(): Unit = {
    val username = Option(usernameField.getText).map(_.trim).getOrElse("")
    println(s"Attempting login for username: $username")

    if (username.isEmpty) {
      errorLabel.setText("Username cannot be empty!")
      errorLabel.setVisible(true)
      return
    }

    // Akka interaction for login
    implicit val timeout: Timeout = Timeout(5.seconds)
    val userManagerActor = MainApp.userManagerActor

    val loginResult: Future[Boolean] = (userManagerActor ? Messages.LoginUser(User(username), self)).mapTo[Boolean]

    loginResult.onComplete {
      case Success(true) =>
        println(s"Login successful for user: $username")
        val user = User(username, playlists = List.empty)
        Platform.runLater(() => mainApp.showMainPage(user))

      case Success(false) =>
        println(s"Login failed for user: $username")
        Platform.runLater(() => {
          errorLabel.setText("Login failed! Username already exists.")
          errorLabel.setVisible(true)
        })

      case Failure(ex) =>
        println(s"Error during login: ${ex.getMessage}")
        Platform.runLater(() => {
          errorLabel.setText("An error occurred. Please try again.")
          errorLabel.setVisible(true)
        })
    }
  }

}
