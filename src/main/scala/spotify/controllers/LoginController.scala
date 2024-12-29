package controllers

import spotify.MainApp
import javafx.fxml.FXML
import javafx.scene.control.{Button, Label, ProgressIndicator, TextField}
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import javafx.application.Platform
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import actors.Messages
import spotify.models.User

class LoginController {

  // FXML references
  @FXML private var usernameField: TextField = _
  @FXML private var errorLabel: Label = _
  @FXML private var loadingIndicator: ProgressIndicator = _
  @FXML private var loginButton: Button = _

  // Akka Actor System
  private val system: ActorSystem = MainApp.actorSystem
  private var userActor = system.actorOf(actors.UserManagerActor.props(), "UserManagerActor")

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

    // Ensure MainApp reference is set
    if (mainApp == null) {
      println("Error: MainApp reference is null.")
      errorLabel.setText("Application error! Please restart.")
      errorLabel.setVisible(true)
      return
    }

    // Ensure userActor is initialized
    if (userActor == null) {
      println("Error: userActor is null.")
      errorLabel.setText("Application error! Please restart.")
      errorLabel.setVisible(true)
      return
    }

    // Hide error label and show loading indicator
    errorLabel.setVisible(false)
    loadingIndicator.setVisible(true)

    // Akka interaction for login
    implicit val timeout: Timeout = Timeout(5.seconds)
    val loginResult: Future[Boolean] = (userActor ? Messages.LoginUser(username)).mapTo[Boolean]

    loginResult.onComplete {
      case Success(true) =>
        // Successfully logged in
        val user = User(username, playlists = List.empty) // Create a new user instance
        println(s"Login successful for user: $username")

        // Use Platform.runLater to ensure UI update happens on JavaFX thread
        Platform.runLater(new Runnable {
          override def run(): Unit = {
            mainApp.showMainPage(user)
          }
        })

      case Success(false) =>
        // Login failed
        println(s"Login failed for user: $username (username already exists).")
        loadingIndicator.setVisible(false)
        errorLabel.setText("Login failed! Username already exists.")
        errorLabel.setVisible(true)

      case Failure(ex) =>
        // Unexpected error
        println(s"Error during login process: ${ex.getMessage}")
        ex.printStackTrace()
        loadingIndicator.setVisible(false)
        errorLabel.setText("An error occurred during login. Please try again.")
        errorLabel.setVisible(true)
    }
  }
}
