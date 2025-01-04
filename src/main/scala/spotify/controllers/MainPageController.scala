package spotify.controllers

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.control._
import javafx.collections.{FXCollections, ObservableList}
import spotify.models.{Playlist, Song, User}
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

  private val system: ActorSystem[Messages.Command] = MainApp.actorSystem
  private val playlistActor: ActorRef[Messages.Command] = MainApp.playlistActor
  private val songDatabaseActor: ActorRef[Messages.Command] = MainApp.songDatabaseActor

  implicit val timeout: Timeout = 5.seconds
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
    loadSongs()
    loadGenres()
  }

  private def loadGenres(): Unit = {
    val replyActor = system.systemActorOf(
      Behaviors.receiveMessage[List[String]] { genres =>
        Platform.runLater(() => {
          genreFilter.getItems.clear()
          genreFilter.getItems.addAll(genres: _*)
        })
        Behaviors.stopped
      },
      "GenreReplyActor"
    )

    songDatabaseActor ! Messages.GetGenres(replyActor)
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
    playlistActor ! Messages.GetPlaylist(currentUser.username, system.systemActorOf(
      Behaviors.receiveMessage[Playlist] { playlist =>
        Platform.runLater(() => setCurrentPlaylist(playlist))
        Behaviors.stopped
      },
      "PlaylistReplyActor"
    ))
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

    val replyActor = system.systemActorOf(
      Behaviors.receiveMessage[List[Song]] { songList =>
        Platform.runLater(() => {
          songs.clear()
          songs.addAll(songList: _*)
          println(s"Loaded ${songList.size} songs into the table.")
        })
        Behaviors.stopped
      },
      "SongLoadReplyActor"
    )

    songDatabaseActor ! Messages.SearchSongs("", replyActor)
  }

  @FXML
  private def handleSearch(): Unit = {
    val query = searchField.getText.trim
    val genre = Option(genreFilter.getValue)
    println(s"Searching for songs with query: '$query' and genre: $genre")

    val replyActor = system.systemActorOf(
      Behaviors.receiveMessage[List[Song]] { searchResults =>
        Platform.runLater(() => {
          songs.clear()
          songs.addAll(searchResults: _*)
          println(s"Search returned ${searchResults.size} songs.")
        })
        Behaviors.stopped
      },
      "SearchReplyActor"
    )

    genre match {
      case Some(g) if g.nonEmpty =>
        songDatabaseActor ! Messages.SearchSongsWithGenre(query, g, replyActor)
      case _ =>
        songDatabaseActor ! Messages.SearchSongs(query, replyActor)
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

    val replyActor = system.systemActorOf(
      Behaviors.receiveMessage[Boolean] { success =>
        Platform.runLater(() => {
          if (success) {
            println("Song added successfully.")
            fetchPlaylistForUser()
          } else {
            println("Failed to add song to playlist.")
            val alert = new Alert(Alert.AlertType.ERROR)
            alert.setTitle("Error")
            alert.setHeaderText("Failed to add song")
            alert.setContentText("Unable to add song to playlist.")
            alert.showAndWait()
          }
        })
        Behaviors.stopped
      },
      "AddSongReplyActor"
    )

    playlistActor ! Messages.AddSongToPlaylist(
      currentUser.username,
      selectedSong,
      currentPlaylist.id,
      replyActor
    )
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