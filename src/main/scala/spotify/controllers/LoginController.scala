package spotify.controllers

import spotify.MainApp
import javafx.fxml.FXML
import javafx.scene.control.{Button, Label, ProgressIndicator, TextField}
import akka.actor.typed.ActorRef  // Use Akka Typed's ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import spotify.actors.Messages
import spotify.models.User

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import javafx.application.Platform

class LoginController {

  @FXML private var usernameField: TextField = _
  @FXML private var errorLabel: Label = _
  @FXML private var loadingIndicator: ProgressIndicator = _
  @FXML private var loginButton: Button = _

  private var mainApp: MainApp = _
  private var userManagerActor: ActorRef[Messages.Command] = _

  @FXML
  def initialize(): Unit = {
    require(usernameField != null, "usernameField not injected by FXML")
    require(errorLabel != null, "errorLabel not injected by FXML")
    require(loadingIndicator != null, "loadingIndicator not injected by FXML")
    require(loginButton != null, "loginButton not injected by FXML")

    loadingIndicator.setVisible(false)
    errorLabel.setVisible(false)
    println("LoginController initialized successfully")
  }

  def setApp(app: MainApp): Unit = {
    require(app != null, "MainApp cannot be null")
    this.mainApp = app
    this.userManagerActor = MainApp.userManagerActor  // Use the actor reference from MainApp
    println(s"MainApp reference set in LoginController: $mainApp")
  }

  @FXML
  private def handleLogin(): Unit = {
    val username = Option(usernameField.getText).map(_.trim).getOrElse("")
    println(s"Attempting login for username: $username")

    if (username.isEmpty) {
      showError("Username cannot be empty!")
      return
    }

    if (mainApp == null) {
      showError("System error: MainApp not initialized")
      return
    }

    showLoading(true)

    implicit val timeout: Timeout = Timeout(5.seconds)
    implicit val scheduler = MainApp.actorSystem.scheduler  // Get the scheduler from the actor system

    // Send the login request to the UserManagerActor
    val loginRequest: Future[Boolean] = (userManagerActor ? (ref => Messages.LoginUser(User(username), ref)))
      .mapTo[Boolean]

    loginRequest.onComplete {
      case Success(true) =>
        println(s"Login successful for user: $username")
        val user = User(username, playlists = List.empty)
        updateUIAfterLogin(user)

      case Success(false) =>
        println(s"Login failed for user: $username")
        updateUIAfterFailure("Login failed! Username already exists.")

      case Failure(ex) =>
        println(s"Error during login: ${ex.getMessage}")
        ex.printStackTrace()
        updateUIAfterFailure("An error occurred. Please try again.")
    }
  }

  private def updateUIAfterLogin(user: User): Unit = {
    Platform.runLater(() => {
      showLoading(false)
      mainApp.showMainPage(user)
    })
  }

  private def updateUIAfterFailure(message: String): Unit = {
    Platform.runLater(() => {
      showLoading(false)
      showError(message)
    })
  }

  private def showError(message: String): Unit = {
    if (errorLabel != null) {
      errorLabel.setText(message)
      errorLabel.setVisible(true)
    }
  }

  private def showLoading(isLoading: Boolean): Unit = {
    if (loadingIndicator != null && loginButton != null) {
      loadingIndicator.setVisible(isLoading)
      loginButton.setDisable(isLoading)
    }
  }
}
