package spotify.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.eventstream.EventStream
import spotify.models.{Playlist, Song}
import spotify.actors.Messages._

object PlaylistActor {
  private val SHARED_PLAYLIST_ID = "shared"

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val initialPlaylist = Playlist(SHARED_PLAYLIST_ID, "Shared Playlist")
    var sharedPlaylist = initialPlaylist

    Behaviors.receiveMessage {
      case GetPlaylist(username, replyTo) =>
        context.log.info(s"Sending shared playlist to user '$username' with ${sharedPlaylist.songs.size} songs")
        replyTo ! sharedPlaylist
        Behaviors.same

      case AddSongToPlaylist(user, song, _, replyTo) =>
        val updatedSong = song.copy(contributor = Some(user))
        val updatedPlaylist = sharedPlaylist.addSong(updatedSong)
        sharedPlaylist = updatedPlaylist

        context.log.info(s"Song '${song.title}' added to shared playlist by user '$user'. Total songs: ${updatedPlaylist.songs.size}")

        // Use the Messages.PlaylistUpdated event
        context.system.eventStream.tell(EventStream.Publish(PlaylistUpdated(updatedPlaylist)))

        replyTo ! true
        Behaviors.same

      case RemoveSongFromPlaylist(user, songId, _, replyTo) =>
        val updatedPlaylist = sharedPlaylist.removeSong(songId)
        sharedPlaylist = updatedPlaylist

        context.log.info(s"Song '$songId' removed from shared playlist by user '$user'. Total songs: ${updatedPlaylist.songs.size}")

        context.system.eventStream.tell(EventStream.Publish(PlaylistUpdated(updatedPlaylist)))

        replyTo ! true
        Behaviors.same

      case _ => Behaviors.same
    }
  }
}