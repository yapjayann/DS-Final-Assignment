package actors

import akka.actor.Actor
import spotify.models.Song
import actors.Messages._

class PlayerActor extends Actor {
  private var playbackQueue: List[Song] = List.empty // Queue of songs
  private var currentSong: Option[Song] = None
  private var isPlaying: Boolean = false

  override def receive: Receive = {
    case PlaySong =>
      if (currentSong.isDefined) {
        isPlaying = true
        sender() ! s"Playing song: ${currentSong.get.title} by ${currentSong.get.artist}"
      } else {
        sender() ! "No song is currently loaded to play."

      }

    case Pause =>
      if (isPlaying) {
        isPlaying = false
        sender() ! "Playback paused."
      } else {
        sender() ! "Playback is already paused."
      }

    case Next =>
      if (playbackQueue.nonEmpty) {
        currentSong = Some(playbackQueue.head)
        playbackQueue = playbackQueue.tail
        sender() ! s"Playing next song: ${currentSong.get.title} by ${currentSong.get.artist}"
      } else {
        currentSong = None
        sender() ! "No more songs in the queue."
      }

    case Previous =>
      sender() ! "Previous functionality is not implemented yet."

    case AddSongToPlaylist(_, song: Song) =>
      playbackQueue :+= song
      if (currentSong.isEmpty) {
        currentSong = Some(song)
        sender() ! s"Started playing: ${song.title} by ${song.artist}"
      } else {
        sender() ! s"Song added to queue: ${song.title} by ${song.artist}"
      }

    case RemoveSongFromPlaylist(_, songId) =>
      val initialSize = playbackQueue.size
      playbackQueue = playbackQueue.filterNot(_.id == songId)
      if (currentSong.exists(_.id == songId)) {
        currentSong = playbackQueue.headOption
        playbackQueue = playbackQueue.drop(1)
        sender() ! s"Current song removed. Now playing: ${currentSong.map(_.title).getOrElse("None")}"
      } else if (initialSize != playbackQueue.size) {
        sender() ! s"Song with ID: $songId removed from the queue."
      } else {
        sender() ! s"No song with ID: $songId found in the queue."
      }

    case GetPlaylist =>
      sender() ! playbackQueue

    case _ =>
      sender() ! "Unknown message received by PlayerActor."
  }
}
