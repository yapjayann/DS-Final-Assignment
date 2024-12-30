package actors

import akka.actor.{Actor, Props}
import actors.Messages._
import spotify.models.{Playlist, Song}
import java.nio.file.{Files, Paths}
import java.io.{ObjectOutputStream, ObjectInputStream}

class PlaylistActor extends Actor {
  private var playlist: Playlist = loadPlaylist()
  private var contributors: Set[String] = Set.empty
  private var currentSong: Option[Song] = None
  private var songQueue: List[Song] = List.empty

  override def receive: Receive = {
    case AddSongToPlaylist(user, song: Song) =>
      if (contributors.contains(user)) {
        val songWithContributor = song.copy(contributor = Some(user))
        playlist = playlist.addSong(songWithContributor)
        savePlaylist()
        sender() ! true
      } else {
        sender() ! false
      }

    case RemoveSongFromPlaylist(user, songId) =>
      if (contributors.contains(user)) {
        playlist = playlist.removeSong(songId)
        savePlaylist()
        sender() ! true
      } else {
        sender() ! false
      }

    case AddContributor(username) =>
      contributors += username
      sender() ! true

    case GetPlaylist =>
      sender() ! playlist

    case AddToQueue(song: Song) =>
      songQueue = songQueue :+ song
      if (currentSong.isEmpty) {
        playNextSongInQueue()
      }
      sender() ! songQueue

    case GetQueue =>
      sender() ! songQueue

    case PlaySong(song: Song) =>
      currentSong = Some(song)
      sender() ! currentSong

    case SkipToNext =>
      playNextSongInQueue()

    case _ => println("Unknown message received.")
  }

  // Play the next song in the queue
  private def playNextSongInQueue(): Unit = {
    if (songQueue.nonEmpty) {
      currentSong = Some(songQueue.head)
      songQueue = songQueue.tail
      println(s"Playing next song: ${currentSong.get.title}")
    } else {
      currentSong = None
      println("Queue is empty. No song to play.")
    }
  }

  // Load the playlist from file (or use default if not found)
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
      Playlist("1", "Shared Playlist")
    }
  }

  // Save the playlist to a file
  private def savePlaylist(): Unit = {
    val filePath = "playlist.dat"
    val outStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(filePath)))
    try {
      outStream.writeObject(playlist)
    } finally {
      outStream.close()
    }
  }
}

object PlaylistActor {
  def props(): Props = Props[PlaylistActor]
}
