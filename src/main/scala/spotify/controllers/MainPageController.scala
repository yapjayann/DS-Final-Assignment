package controllers

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.collections.{FXCollections, ObservableList}
import spotify.models.{Song, User, Playlist}
import akka.actor.{ActorSelection, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import spotify.actors.Messages
import spotify.MainApp

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class MainPageController {

  @FXML private var genreFilter: ComboBox[String] = _
  @FXML private var searchField: TextField = _
  @FXML private var songTable: TableView[Song] = _
  @FXML private var titleColumn: TableColumn[Song, String] = _
  @FXML private var artistColumn: TableColumn[Song, String] = _
  @FXML private var genreColumn: TableColumn[Song, String] = _
  @FXML private var durationColumn: TableColumn[Song, String] = _
  @FXML private var addToPlaylistButton: Button = _
  @FXML private var goToPlaylistButton: Button = _

  // Use the existing actor system from MainApp
  private val system: ActorSystem = MainApp.actorSystem

  // Reference the server-side PlaylistActor and SongDatabaseActor via actorSelection
  private val playlistActor: ActorSelection = system.actorSelection("akka://SpotifySystem@192.168.0.3:2551/user/PlaylistActor")
  private val songDatabaseActor: ActorSelection = system.actorSelection("akka://SpotifySystem@192.168.0.3:2551/user/SongDatabaseActor")

  implicit val timeout: Timeout = Timeout(5.seconds)

  private var currentUser: User = _
  private var currentPlaylist: Playlist = _
  private val songs: ObservableList[Song] = FXCollections.observableArrayList()
  private var mainApp: MainApp = _

  def initialize(): Unit = {
    println("Initializing MainPageController...")

    titleColumn.setCellValueFactory(cellData => cellData.getValue.titleProperty())
    artistColumn.setCellValueFactory(cellData => cellData.getValue.artistProperty())
    genreColumn.setCellValueFactory(cellData => cellData.getValue.genreProperty())
    durationColumn.setCellValueFactory(cellData => cellData.getValue.durationProperty())

    songTable.setItems(songs)

    // Debug: Verify actor paths
    println(s"PlaylistActor path: ${playlistActor.pathString}")
    println(s"SongDatabaseActor path: ${songDatabaseActor.pathString}")

    loadSongs()
  }

  def setCurrentUser(user: User): Unit = {
    if (user == null) {
      println("Error: User is null!")
      return
    }
    currentUser = user
    println(s"Current user set to: ${user.username}")
    fetchPlaylistForUser()
  }

  private def fetchPlaylistForUser(): Unit = {
    println(s"Fetching playlist for user: ${currentUser.username}")

    val replyActor = system.actorOf(Props(new akka.actor.Actor {
      override def receive: Receive = {
        case playlist: Playlist =>
          Platform.runLater(() => setCurrentPlaylist(playlist))
          println(s"Retrieved playlist for user: ${playlist.name}")
          context.stop(self) // Stop the temporary actor after use
        case _ =>
          println("Failed to fetch playlist.")
          context.stop(self) // Stop the temporary actor after use
      }
    }))

    // Send request to the playlist actor
    playlistActor ! Messages.GetPlaylist(currentUser.username, replyActor)
  }

  def setCurrentPlaylist(playlist: Playlist): Unit = {
    if (playlist == null) {
      println("Error: Playlist is null!")
      return
    }
    currentPlaylist = playlist
    println(s"Current playlist set to: ${playlist.name}")
  }

  def setMainApp(mainApp: MainApp): Unit = {
    this.mainApp = mainApp
  }

  private def loadSongs(): Unit = {
    println("Loading songs from SongDatabaseActor...")

    // Test communication with SongDatabaseActor
    songDatabaseActor ! Messages.SearchSongs("")

    (songDatabaseActor ? Messages.SearchSongs("")).mapTo[List[Song]].onComplete {
      case Success(songList) =>
        Platform.runLater(() => {
          songs.clear()
          songs.addAll(songList: _*)
          println(s"Loaded ${songList.size} songs into the table.")
        })
      case Failure(exception) =>
        println(s"Error loading songs: ${exception.getMessage}")
    }
  }

  @FXML
  private def handleSearch(): Unit = {
    val query = searchField.getText.trim
    val genre = genreFilter.getValue
    println(s"Searching for songs with query: '$query' and genre: '$genre'")

    (songDatabaseActor ? Messages.SearchSongsWithGenre(query, genre)).mapTo[List[Song]].onComplete {
      case Success(searchResults) =>
        Platform.runLater(() => {
          songs.clear()
          songs.addAll(searchResults: _*)
          println(s"Search returned ${searchResults.size} songs.")
        })
      case Failure(exception) =>
        println(s"Error searching songs: ${exception.getMessage}")
    }
  }

  @FXML
  private def handleAddToPlaylist(): Unit = {
    if (currentUser == null || currentPlaylist == null) {
      println("Error: User or playlist is not set!")
      return
    }
    val selectedSong = songTable.getSelectionModel.getSelectedItem
    if (selectedSong == null) {
      println("No song selected to add to playlist.")
      return
    }
    println(s"Adding song to playlist: ${selectedSong.title}")

    val replyActor = system.actorOf(Props(new akka.actor.Actor {
      override def receive: Receive = {
        case success: Boolean if success =>
          println(s"Song '${selectedSong.title}' added to playlist successfully.")
          fetchPlaylistForUser()
          context.stop(self) // Stop the temporary actor after use
        case _ =>
          println(s"Failed to add song '${selectedSong.title}' to playlist.")
          context.stop(self) // Stop the temporary actor after use
      }
    }))

    playlistActor ! Messages.AddSongToPlaylist(currentUser.username, selectedSong, currentPlaylist.id, replyActor)
  }

  @FXML
  private def handleGoToPlaylist(): Unit = {
    if (currentUser == null || currentPlaylist == null) {
      println("Error: User or playlist is not set!")
      return
    }
    println("Navigating to Playlist Screen...")
    mainApp.showPlaylistScreen(currentUser, currentPlaylist)
  }
}
