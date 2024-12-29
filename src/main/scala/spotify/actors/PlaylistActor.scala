package actors

import akka.actor.{Actor, Props}
import actors.Messages._
import spotify.models.{Playlist, Song}
import java.nio.file.{Files, Paths}
import java.io.{ObjectOutputStream, ObjectInputStream}

class PlaylistActor extends Actor {
  private var playlist: Playlist = loadPlaylist()
  private var contributors: Set[String] = Set.empty
  private var currentSong: Option[Song] = None  // Track the current song being played

  override def receive: Receive = {
    case AddSongToPlaylist(user, song: Song) =>
      if (contributors.contains(user)) {
        val songWithContributor = song.copy(contributor = Some(user))  // Assuming Song has contributor field
        playlist = playlist.addSong(songWithContributor)
        savePlaylist()  // Save the playlist after adding a song
        sender() ! true
      } else {
        sender() ! false
      }

    case RemoveSongFromPlaylist(user, songId) =>
      if (contributors.contains(user)) {
        playlist = playlist.removeSong(songId)
        savePlaylist()  // Save the playlist after removing a song
        sender() ! true
      } else {
        sender() ! false
      }

    case AddContributor(username) =>
      contributors += username
      sender() ! true

    case GetPlaylist =>
      sender() ! playlist

    case PlaySong(song: Song) =>
      // Stop the current song if it's already playing
      stopCurrentSong()

      // Play the new song
      currentSong = Some(song)
      playSong(song)
      sender() ! s"Now playing: ${song.title}"

    case StopSong =>
      stopCurrentSong()
      sender() ! "Playback stopped."

  }

  // Method to stop the current song if any
  private def stopCurrentSong(): Unit = {
    currentSong match {
      case Some(song) =>
        // Logic to stop the current song
        stopSong(song)
        currentSong = None // Reset the current song
      case None =>
        // If no song is playing, nothing to stop
        println("No song is currently playing.")
    }
  }

  // Method to save the playlist to a file
  private def savePlaylist(): Unit = {
    val filePath = "playlist.dat"
    val outStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath)))
    try {
      outStream.writeObject(playlist)
    } finally {
      outStream.close()
    }
  }

  // Method to load the playlist from a file
  private def loadPlaylist(): Playlist = {
    val filePath = "playlist.dat"
    if (Files.exists(Paths.get(filePath))) {
      val inStream = new ObjectInputStream(Files.newInputStream(Paths.get(filePath)))
      try {
        inStream.readObject().asInstanceOf[Playlist]
      } catch {
        case _: Exception => Playlist("1", "Shared Playlist")  // Default playlist if no file
      } finally {
        inStream.close()
      }
    } else {
      Playlist("1", "Shared Playlist")  // Default playlist if no file
    }
  }

  // Method to simulate playing a song
  private def playSong(song: Song): Unit = {
    // Simulate the song being played
    println(s"Playing song: ${song.title}")
  }

  // Method to simulate stopping a song
  private def stopSong(song: Song): Unit = {
    // Simulate the song being stopped
    println(s"Stopping song: ${song.title}")
  }
}

object PlaylistActor {
  def props(): Props = Props[PlaylistActor]
}
