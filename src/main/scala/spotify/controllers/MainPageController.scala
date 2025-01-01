package controllers

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.collections.{FXCollections, ObservableList}
import spotify.models.{Song, User, Playlist}
import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import spotify.actors.{Messages, SongDatabaseActor, PlaylistActor}
import spotify.MainApp

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class MainPageController {

  // FXML references
  @FXML private var genreFilter: ComboBox[String] = _
  @FXML private var searchField: TextField = _
  @FXML private var songTable: TableView[Song] = _
  @FXML private var titleColumn: TableColumn[Song, String] = _
  @FXML private var artistColumn: TableColumn[Song, String] = _
  @FXML private var genreColumn: TableColumn[Song, String] = _
  @FXML private var durationColumn: TableColumn[Song, String] = _
  @FXML private var addToPlaylistButton: Button = _
  @FXML private var goToPlaylistButton: Button = _

  // Akka Actor System
  private val system: ActorSystem = ActorSystem("SpotifySystem")
  private var songDatabaseActor: Option[ActorRef] = None
  private var playlistActor: Option[ActorRef] = None

  // Timeout for Akka messaging
  implicit val timeout: Timeout = Timeout(5.seconds)

  // Current user and playlist
  private var currentUser: User = _
  private var currentPlaylist: Playlist = _

  // Observable list for songs in the table
  private val songs: ObservableList[Song] = FXCollections.observableArrayList()

  // Reference to MainApp for screen navigation
  private var mainApp: MainApp = _

  def initialize(): Unit = {
    println("Initializing MainPageController...")

    // Initialize actors
    songDatabaseActor = Some(system.actorOf(SongDatabaseActor.props(), "SongDatabaseActor"))
    playlistActor = Some(system.actorOf(PlaylistActor.props(), "PlaylistActor"))

    // Configure table columns
    titleColumn.setCellValueFactory(cellData => cellData.getValue.titleProperty())
    artistColumn.setCellValueFactory(cellData => cellData.getValue.artistProperty())
    genreColumn.setCellValueFactory(cellData => cellData.getValue.genreProperty())
    durationColumn.setCellValueFactory(cellData => cellData.getValue.durationProperty())

    // Set the table's items
    songTable.setItems(songs)

    // Load songs into the table
    loadSongs()
  }

  def setCurrentUser(user: User): Unit = {
    if (user == null) {
      throw new IllegalArgumentException("User cannot be null in setCurrentUser")
    }
    this.currentUser = user
    println(s"Current user set to: ${user.username}")

    // Initialize playlist for this user
    playlistActor.foreach { actor =>
      val playlistId = currentPlaylist.id // Assuming `currentPlaylist` is already set

      // Use ask pattern to handle `replyTo` automatically
      implicit val timeout: Timeout = Timeout(5.seconds)
      val addContributorFuture: Future[Boolean] = actor.ask(replyTo =>
        Messages.AddContributor(user.username, playlistId, replyTo)
      ).mapTo[Boolean]

      addContributorFuture.map { success =>
        if (success) {
          println(s"Added ${user.username} as contributor to playlist $playlistId")
          // Retrieve the playlist
          actor.ask(replyTo => Messages.GetPlaylist(replyTo)).mapTo[Playlist].map { playlist =>
            setCurrentPlaylist(playlist)
            println(s"Retrieved playlist for user: ${playlist.name}")
          }
        } else {
          println(s"Failed to add ${user.username} as contributor to playlist $playlistId")
        }
      }.recover {
        case ex: Exception =>
          println(s"Error adding contributor: ${ex.getMessage}")
      }
    }
  }


  def setCurrentPlaylist(playlist: Playlist): Unit = {
    if (playlist == null) {
      throw new IllegalArgumentException("Playlist cannot be null in setCurrentPlaylist")
    }
    this.currentPlaylist = playlist
    println(s"Current playlist set to: ${playlist.name}")
  }

  def setMainApp(mainApp: MainApp): Unit = {
    this.mainApp = mainApp
  }

  private def loadSongs(): Unit = {
    println("Loading songs...")

    songDatabaseActor match {
      case Some(actor) =>
        (actor ? Messages.SearchSongs("")).mapTo[List[Song]].map { songsList =>
          songs.clear()
          songs.addAll(songsList: _*)
        }
      case None =>
        println("SongDatabaseActor is not initialized!")
    }
  }

  @FXML
  private def handleSearch(): Unit = {
    val query = searchField.getText.trim
    val genre = genreFilter.getValue

    println(s"Searching for songs with query: '$query' and genre: '$genre'")
    songDatabaseActor match {
      case Some(actor) =>
        (actor ? Messages.SearchSongsWithGenre(query, genre)).mapTo[List[Song]].map { searchResults =>
          songs.clear()
          songs.addAll(searchResults: _*)
        }
      case None =>
        println("SongDatabaseActor is not initialized!")
    }
  }

  @FXML
  private def handleAddToPlaylist(): Unit = {
    val selectedSong = songTable.getSelectionModel.getSelectedItem
    if (selectedSong == null) {
      println("No song selected to add to playlist.")
      return
    }

    println(s"Adding song to playlist: ${selectedSong.title}")
    playlistActor match {
      case Some(actor) =>
        val playlistId = currentPlaylist.id // Assuming `currentPlaylist` is already set
        (actor ? Messages.AddSongToPlaylist(currentUser.username, selectedSong, playlistId, actor)).mapTo[Boolean].map { success =>
          if (success) {
            println(s"Song '${selectedSong.title}' added to playlist $playlistId successfully.")
            updatePlaylistTable()
          } else {
            println(s"Failed to add song '${selectedSong.title}' to playlist $playlistId.")
          }
        }
      case None =>
        println("PlaylistActor is not initialized!")
    }
  }


  // Update the playlist table after adding a song
  private def updatePlaylistTable(): Unit = {
    playlistActor.foreach { actor =>
      (actor ? Messages.GetPlaylist).mapTo[Playlist].map { playlist =>
        currentPlaylist = playlist
        // Ensure UI updates are on the JavaFX application thread
        Platform.runLater(new Runnable {
          override def run(): Unit = {
            mainApp.showPlaylistScreen(currentUser, currentPlaylist)
          }
        })
      }
    }
  }

  @FXML
  private def handleGoToPlaylist(): Unit = {
    println("Navigating to playlist screen...")
    if (currentUser == null) {
      println("Error: Current user is not set!")
      return
    }
    if (currentPlaylist == null) {
      println("Error: Current playlist is not set!")
      return
    }
    mainApp.showPlaylistScreen(currentUser, currentPlaylist)
  }
}