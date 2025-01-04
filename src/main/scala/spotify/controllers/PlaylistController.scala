package spotify.controllers

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import javafx.fxml.{FXML}
import javafx.scene.control._
import javafx.scene.control.cell.PropertyValueFactory
import javafx.collections.{FXCollections, ObservableList}
import spotify.models.{Playlist, User, Song}
import spotify.actors.Messages
import spotify.MainApp
import javafx.scene.media.{Media, MediaPlayer}
import javafx.application.Platform
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import java.nio.file.{Files, Paths}
import java.io.{ObjectOutputStream, ObjectInputStream}

class PlaylistController {
  // FXML references
  @FXML private var currentSongLabel: Label = _
  @FXML private var currentArtistLabel: Label = _
  @FXML private var progressSlider: Slider = _
  @FXML private var volumeSlider: Slider = _
  @FXML private var playlistTable: TableView[Song] = _
  @FXML private var titleColumn: TableColumn[Song, String] = _
  @FXML private var artistColumn: TableColumn[Song, String] = _
  @FXML private var durationColumn: TableColumn[Song, String] = _
  @FXML private var contributorColumn: TableColumn[Song, String] = _
  @FXML private var queueList: ListView[String] = _
  @FXML private var contributorField: TextField = _
  @FXML private var addContributorButton: Button = _
  @FXML private var addSongButton: Button = _
  @FXML private var removeSongButton: Button = _

  // Current state
  private var currentUser: User = _
  private var currentPlaylist: Playlist = _
  private var mainApp: MainApp = _
  private var mediaPlayer: MediaPlayer = _

  // Actor system references
  private val system: ActorSystem[Messages.Command] = MainApp.actorSystem
  private val playlistActor: ActorRef[Messages.Command] = MainApp.playlistActor
  implicit val timeout: Timeout = Timeout(5.seconds)
  implicit val scheduler = system.scheduler

  def initialize(): Unit = {
    titleColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("title"))
    artistColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("artist"))
    durationColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("duration"))
    contributorColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("contributor"))

    volumeSlider.setValue(50.0)
    progressSlider.setValue(0.0)

    // Add listeners for media controls
    volumeSlider.valueProperty.addListener((_, _, newValue) => handleVolumeChange())
    progressSlider.valueProperty.addListener((_, _, newValue) => handleProgressChange())

    println("PlaylistController initialized")
  }

  def setMainApp(mainApp: MainApp): Unit = {
    this.mainApp = mainApp
    println("MainApp reference set in PlaylistController")
  }

  def setCurrentUser(user: User): Unit = {
    if (user != null) {
      this.currentUser = user
      println(s"Current user set to: ${user.username}")
    } else {
      println("Warning: Attempted to set null user")
    }
  }

  def setCurrentUserAndPlaylist(user: User, playlist: Playlist): Unit = {
    setCurrentUser(user)
    if (playlist != null) {
      this.currentPlaylist = playlist
      println(s"Current playlist set to: ${playlist.name}")
      Platform.runLater(() => populatePlaylistTable())
    } else {
      println("Warning: Attempted to set null playlist")
    }
  }

  private def populatePlaylistTable(): Unit = {
    if (currentPlaylist != null) {
      val observableList: ObservableList[Song] = FXCollections.observableArrayList()
      observableList.addAll(currentPlaylist.songs: _*)
      playlistTable.setItems(observableList)
      println(s"Populated playlist table with ${currentPlaylist.songs.size} songs")
    }
  }

  @FXML
  private def handlePlayPause(): Unit = {
    Option(mediaPlayer).foreach { player =>
      if (player.getStatus == MediaPlayer.Status.PLAYING) {
        player.pause()
        println("Paused playback")
      } else {
        player.play()
        println("Started playback")
      }
    }
  }

  @FXML
  private def handlePrevious(): Unit = {
    val currentIndex = playlistTable.getSelectionModel.getSelectedIndex
    if (currentIndex > 0) {
      playlistTable.getSelectionModel.select(currentIndex - 1)
      handlePlaySong()
      println("Playing previous song")
    } else {
      println("No previous song available")
    }
  }

  @FXML
  private def handleNext(): Unit = {
    val currentIndex = playlistTable.getSelectionModel.getSelectedIndex
    if (currentIndex >= 0 && currentIndex < playlistTable.getItems.size() - 1) {
      playlistTable.getSelectionModel.select(currentIndex + 1)
      handlePlaySong()
      println("Playing next song")
    } else {
      println("No next song available")
    }
  }

  @FXML
  private def handleAddContributor(): Unit = {
    val username = contributorField.getText.trim
    if (username.nonEmpty && currentPlaylist != null) {
      println(s"Adding contributor: $username")

      (playlistActor ? (ref => Messages.AddContributor(username, currentPlaylist.id, ref))).mapTo[Boolean]
        .onComplete {
          case Success(true) =>
            Platform.runLater(() => {
              contributorField.clear()
              println(s"Successfully added contributor: $username")
            })
          case Success(false) =>
            println(s"Failed to add contributor: $username")
          case Failure(ex) =>
            println(s"Error adding contributor: ${ex.getMessage}")
        }
    }
  }

  @FXML
  private def handlePlaySong(): Unit = {
    Option(playlistTable.getSelectionModel.getSelectedItem).foreach { selectedSong =>
      // Stop current playback if any
      Option(mediaPlayer).foreach(_.stop())

      try {
        val media = new Media(selectedSong.filePath)
        mediaPlayer = new MediaPlayer(media)
        mediaPlayer.setVolume(volumeSlider.getValue / 100)

        // Update UI
        currentSongLabel.setText(selectedSong.title)
        currentArtistLabel.setText(selectedSong.artist)
        progressSlider.setMax(selectedSong.duration.toDouble)

        // Add listeners
        mediaPlayer.currentTimeProperty.addListener((_, _, newValue) =>
          Platform.runLater(() => progressSlider.setValue(newValue.toSeconds))
        )

        mediaPlayer.setOnEndOfMedia(() => {
          println("Song finished playing")
          handleNext()
        })

        mediaPlayer.play()
        println(s"Now playing: ${selectedSong.title} by ${selectedSong.artist}")
      } catch {
        case ex: Exception =>
          println(s"Error playing song: ${ex.getMessage}")
      }
    }
  }

  @FXML
  private def handleRemoveSong(): Unit = {
    if (currentUser == null || currentPlaylist == null) {
      println("Error: User or playlist not set")
      return
    }

    Option(playlistTable.getSelectionModel.getSelectedItem).foreach { selectedSong =>
      (playlistActor ? (ref =>
        Messages.RemoveSongFromPlaylist(currentUser.username, selectedSong.id, currentPlaylist.id, ref)
        )).mapTo[Boolean].onComplete {
        case Success(true) =>
          Platform.runLater(() => {
            populatePlaylistTable()
            println(s"Successfully removed song: ${selectedSong.title}")
          })
        case Success(false) =>
          println(s"Failed to remove song: ${selectedSong.title}")
        case Failure(ex) =>
          println(s"Error removing song: ${ex.getMessage}")
      }
    }
  }

  @FXML
  private def handleGoToBrowse(): Unit = {
    Option(mainApp).foreach { app =>
      println("Navigating to MainPage (Browse)")
      app.showMainPage(currentUser)
    }
  }

  @FXML
  private def handleVolumeChange(): Unit = {
    Option(mediaPlayer).foreach { player =>
      val volume = volumeSlider.getValue / 100
      player.setVolume(volume)
      println(s"Volume set to: ${volume * 100}%")
    }
  }

  @FXML
  private def handleProgressChange(): Unit = {
    Option(mediaPlayer).foreach { player =>
      val progress = progressSlider.getValue
      player.seek(javafx.util.Duration.seconds(progress))
      println(s"Playback position set to: $progress seconds")
    }
  }

  @FXML
  private def handleAddToQueue(): Unit = {
    Option(playlistTable.getSelectionModel.getSelectedItem).foreach { selectedSong =>
      queueList.getItems.add(s"${selectedSong.title} - ${selectedSong.artist}")
      println(s"Added to queue: ${selectedSong.title}")
    }
  }

  private def savePlaylistToFile(playlist: Playlist): Unit = {
    try {
      val filePath = s"playlist_${playlist.id}.dat"
      val outStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath)))
      try {
        outStream.writeObject(playlist)
        println(s"Playlist saved to file: $filePath")
      } finally {
        outStream.close()
      }
    } catch {
      case ex: Exception =>
        println(s"Error saving playlist: ${ex.getMessage}")
    }
  }
}