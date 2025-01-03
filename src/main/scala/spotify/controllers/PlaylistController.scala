package controllers

import javafx.fxml.{FXML}
import javafx.scene.control._
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.AnchorPane
import javafx.collections.{FXCollections, ObservableList}
import spotify.models.{Playlist, User, Song}
import spotify.MainApp
import javafx.scene.media.{Media, MediaPlayer}
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

  // Current user and playlist
  private var currentUser: User = _
  private var currentPlaylist: Playlist = _
  private var mainApp: MainApp = _

  // MediaPlayer instance for song playback
  private var mediaPlayer: MediaPlayer = _

  // Initialize the controller
  def initialize(): Unit = {
    // Initialize table columns
    titleColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("title"))
    artistColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("artist"))
    durationColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("duration"))
    contributorColumn.setCellValueFactory(new PropertyValueFactory[Song, String]("contributor"))

    // Initialize sliders
    volumeSlider.setValue(50.0) // Set default volume
    progressSlider.setValue(0.0)
  }

  // Set the main app reference
  def setMainApp(mainApp: MainApp): Unit = {
    this.mainApp = mainApp
  }

  // Set current user for playlist screen
  def setCurrentUser(user: User): Unit = {
    this.currentUser = user
    println(s"Current user set to: ${user.username}")
  }

  // Set current user and playlist for the screen
  def setCurrentUserAndPlaylist(user: User, playlist: Playlist): Unit = {
    this.currentUser = user
    this.currentPlaylist = playlist
    println(s"Current user: ${user.username}, Current playlist: ${playlist.name}")

    // Populate the playlist table
    populatePlaylistTable()
  }

  // Populate the playlist table with songs
  private def populatePlaylistTable(): Unit = {
    if (currentPlaylist != null) {
      val observableList: ObservableList[Song] = FXCollections.observableArrayList()
      observableList.addAll(currentPlaylist.songs: _*)
      playlistTable.setItems(observableList)
    }
  }

  // Handle the play/pause button functionality
  @FXML
  private def handlePlayPause(): Unit = {
    if (mediaPlayer != null) {
      if (mediaPlayer.getStatus == MediaPlayer.Status.PLAYING) {
        mediaPlayer.pause()
        println("Paused the song.")
      } else {
        mediaPlayer.play()
        println("Playing the song.")
      }
    } else {
      println("MediaPlayer is not initialized!")
    }
  }

  // Handle the previous song button
  @FXML
  private def handlePrevious(): Unit = {
    println("Previous button clicked")
    // Implement previous song functionality here
  }

  // Handle the next song button
  @FXML
  private def handleNext(): Unit = {
    val currentIndex = playlistTable.getSelectionModel.getSelectedIndex
    if (currentIndex >= 0 && currentIndex < playlistTable.getItems.size() - 1) {
      val nextSong = playlistTable.getItems.get(currentIndex + 1)
      playlistTable.getSelectionModel.select(nextSong)
      handlePlaySong() // Play the selected next song
      println(s"Playing next song: ${nextSong.title}")
    } else {
      println("No next song available.")
    }
  }

  // Handle adding a contributor to the playlist
  @FXML
  private def handleAddContributor(): Unit = {
    val username = contributorField.getText.trim
    if (username.nonEmpty) {
      println(s"Adding contributor: $username")
      // Implement add contributor functionality here
    } else {
      println("Username is empty!")
    }
  }

  // Handle playing a selected song
  @FXML
  private def handlePlaySong(): Unit = {
    val selectedSong = playlistTable.getSelectionModel.getSelectedItem
    if (selectedSong != null) {
      // If there's already a song playing, stop it
      if (mediaPlayer != null) {
        mediaPlayer.stop()
        println(s"Stopping the current song: ${currentSongLabel.getText}")
      }

      // Set the MediaPlayer to the selected song
      setMediaPlayer(selectedSong)

      // Update UI with song details
      currentSongLabel.setText(selectedSong.title)
      currentArtistLabel.setText(selectedSong.artist)

      // Play the new song
      mediaPlayer.play()

      // Update progress slider max value based on song duration
      progressSlider.setMax(selectedSong.duration.toDouble)

      println(s"Now playing: ${selectedSong.title} by ${selectedSong.artist}")
    } else {
      println("No song selected!")
    }
  }

  // Handle removing a song from the playlist
  @FXML
  private def handleRemoveSong(): Unit = {
    val selectedSong = playlistTable.getSelectionModel.getSelectedItem
    if (selectedSong != null) {
      currentPlaylist = currentPlaylist.removeSong(selectedSong.id) // Update the playlist
      populatePlaylistTable()
      println(s"Removed song: ${selectedSong.title}")

      // Save the updated playlist to persistent storage (file or database)
      savePlaylistToFile(currentPlaylist)
    } else {
      println("No song selected for removal.")
    }
  }

  // Handle navigation to the Browse page
  @FXML
  private def handleGoToBrowse(): Unit = {
    if (mainApp != null) {
      println("Navigating to MainPage (Browse)...")
      mainApp.showMainPage(currentUser)
    } else {
      println("Error: MainApp reference is not set!")
    }
  }

  // Handle volume change via slider
  @FXML
  private def handleVolumeChange(): Unit = {
    if (mediaPlayer != null) {
      mediaPlayer.setVolume(volumeSlider.getValue / 100)
      println(s"Volume set to: ${volumeSlider.getValue}%")
    }
  }

  // Handle progress slider change during song playback
  @FXML
  private def handleProgressChange(): Unit = {
    if (mediaPlayer != null) {
      val progress = progressSlider.getValue
      mediaPlayer.seek(javafx.util.Duration.seconds(progress))
      println(s"Progress set to: $progress seconds")
    }
  }

  private def setMediaPlayer(song: Song): Unit = {
    if (song != null) {
      val media = new Media(song.filePath)
      mediaPlayer = new MediaPlayer(media)
      mediaPlayer.setVolume(volumeSlider.getValue / 100)

      // Update progress slider max value based on song duration
      progressSlider.setMax(song.duration.toDouble)

      // Add event listener for progress update
      mediaPlayer.currentTimeProperty.addListener((_, _, newValue) => {
        progressSlider.setValue(newValue.toSeconds)
      })

      // Add event listener for when the song finishes
      mediaPlayer.setOnEndOfMedia(() => {
        println("Song finished.")
      })
    } else {
      println("No song selected or mediaPlayer not initialized.")
    }
  }

  private def savePlaylistToFile(playlist: Playlist): Unit = {
    val filePath = "playlist.dat"
    val outStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath)))
    try {
      outStream.writeObject(playlist)
    } finally {
      outStream.close()
    }
  }

  // Handle adding a song to the queue
  @FXML
  private def handleAddToQueue(): Unit = {
    val selectedSong = playlistTable.getSelectionModel.getSelectedItem
    if (selectedSong != null) {
      println(s"Added song to queue: ${selectedSong.title}")
    } else {
      println("No song selected to add to the queue.")
    }
  }
}
