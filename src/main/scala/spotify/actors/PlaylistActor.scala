package spotify.actors

import akka.actor.{Actor, Props, ActorRef}
import spotify.models.{Playlist, Song}
import spotify.actors.Messages._

class PlaylistActor extends Actor {
  private var playlist: Playlist = Playlist("1", "Shared Playlist")
  private var contributors: Set[String] = Set.empty
  private var songQueue: List[Song] = List.empty

  override def receive: Receive = {
    case AddSongToPlaylist(user, song, playlistId, replyTo) =>
      if (contributors.contains(user)) {
        playlist = playlist.addSong(song.copy(contributor = Some(user)))
        replyTo ! true
        context.system.log.info(s"Song '${song.title}' added to playlist '$playlistId' by user '$user'.")
      } else {
        replyTo ! false
        context.system.log.info(s"User '$user' is not authorized to add songs to playlist '$playlistId'.")
      }

    case RemoveSongFromPlaylist(user, songId, replyTo) =>
      if (contributors.contains(user)) {
        playlist = playlist.removeSong(songId)
        replyTo ! true
        context.system.log.info(s"Song with ID '$songId' removed by user '$user'.")
      } else {
        replyTo ! false
        context.system.log.info(s"User '$user' is not authorized to remove songs.")
      }

    case AddContributor(username, playlistId, replyTo) =>
      contributors += username
      replyTo ! true
      context.system.log.info(s"User '$username' added as a contributor to playlist '$playlistId'.")

    case GetPlaylist(replyTo) =>
      replyTo ! playlist

    case AddToQueue(song, replyTo) =>
      songQueue :+= song
      replyTo ! true
      context.system.log.info(s"Song '${song.title}' added to the queue.")

    case GetQueue(replyTo) =>
      replyTo ! songQueue

    case _ =>
      context.system.log.warning("Received an unknown message.")
  }
}

object PlaylistActor {
  def props(): Props = Props[PlaylistActor]
}
