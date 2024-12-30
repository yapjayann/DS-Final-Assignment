package controllers

import javafx.fxml.{FXML, FXMLLoader}
import javafx.scene.Scene
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
    populatePlaylistTable()  // Ensure the table is updated with the current playlist
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

  @FXML
  private def handleNext(): Unit = {
    // Check if there's a song in the queue
    if (queueList.getItems.size() > 0) {
      val nextSongTitle = queueList.getItems.get(0)  // Get the title of the next song in the queue
      val nextSong = currentPlaylist.songs.find(_.title == nextSongTitle).getOrElse(null)

      if (nextSong != null) {
        // If the song is found in the playlist, stop the current song
        if (mediaPlayer != null) {
          mediaPlayer.stop()
          println(s"Stopping the current song: ${currentSongLabel.getText}")
        }

        // Set the MediaPlayer to the next song and play it
        setMediaPlayer(nextSong)

        // Update UI with the new song details
        currentSongLabel.setText(nextSong.title)
        currentArtistLabel.setText(nextSong.artist)

        // Play the new song
        mediaPlayer.play()

        // Remove the song from the queue after it starts playing
        queueList.getItems.remove(nextSong.title)

        println(s"Now playing: ${nextSong.title} by ${nextSong.artist}")
      } else {
        println("Song not found in the playlist!")
      }
    } else {
      println("No more songs in the queue.")
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

      // Remove the song from the queue after it starts playing
      queueList.getItems.remove(selectedSong.title)

      println(s"Now playing: ${selectedSong.title} by ${selectedSong.artist}")
    } else {
      println("No song selected!")
    }
  }

  @FXML
  private def handleRemoveSong(): Unit = {
    val selectedSong = playlistTable.getSelectionModel.getSelectedItem
    if (selectedSong != null) {
      currentPlaylist = currentPlaylist.removeSong(selectedSong.id)  // Update the playlist
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

      // Add event listener for progress update (optional)
      mediaPlayer.currentTimeProperty.addListener((_, _, newValue) => {
        progressSlider.setValue(newValue.toSeconds)
      })

      // Add event listener for when the song finishes
      mediaPlayer.setOnEndOfMedia(() => {
        println("Song finished, playing the next one.")
        handleNext() // Automatically play the next song when the current one finishes
      })
    } else {
      println("No song selected or mediaPlayer not initialized.")
    }
  }

  // Handle playing the next song from the queue
  private def handleNextSongFromQueue(): Unit = {
    if (queueList.getItems.size() > 0) {
      // Get the next song from the queue
      val nextSongTitle = queueList.getItems.get(0)
      val nextSong = currentPlaylist.songs.find(_.title == nextSongTitle)

      nextSong match {
        case Some(song) =>
          // Play the next song
          setMediaPlayer(song)
          mediaPlayer.play()
          println(s"Playing next song: ${song.title}")

          // Remove the song from the queue after it starts playing
          queueList.getItems.remove(0)
        case None =>
          println("No song found in the queue to play.")
      }
    } else {
      println("Queue is empty, no next song to play.")
    }
  }

  // Save the updated playlist to persistent storage (file or database)
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
      addSongToQueue(selectedSong)
      println(s"Added song to queue: ${selectedSong.title}")
    } else {
      println("No song selected to add to the queue.")
    }
  }

  // Helper method to add a song to the queue
  private def addSongToQueue(song: Song): Unit = {
    if (song != null) {
      queueList.getItems.add(song.title)
    }
  }
}
