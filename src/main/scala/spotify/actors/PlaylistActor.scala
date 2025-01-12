package spotify.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.eventstream.EventStream
import spotify.models.{Playlist, Song}
import spotify.actors.Messages._

sealed trait PlaylistEvent
case class PlaylistUpdated(playlist: Playlist) extends PlaylistEvent

object PlaylistActor {
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    // Initialize the shared playlist
    val sharedPlaylist = Playlist("1", "Shared Playlist")
    var playlists: Map[String, Playlist] = Map("1" -> sharedPlaylist)

    // Logging to confirm actor setup
    context.log.info("PlaylistActor started and ready to handle commands.")

    Behaviors.receiveMessage {
      case GetPlaylist(_, replyTo) =>
        // Always return the shared playlist
        val playlist = playlists("1")
        replyTo ! playlist
        context.log.info(s"Shared playlist sent to client. Songs count: ${playlist.songs.size}")
        Behaviors.same

      case AddSongToPlaylist(user, song, playlistId, replyTo) =>
        playlists.get(playlistId) match {
          case Some(playlist) =>
            val updatedSong = song.copy(contributor = Some(user))
            val updatedPlaylist = playlist.addSong(updatedSong)
            playlists += (playlistId -> updatedPlaylist)

            // Publish update
            context.system.eventStream ! EventStream.Publish(PlaylistUpdated(updatedPlaylist))

            replyTo ! true
          case None =>
            replyTo ! false
        }
        Behaviors.same

      case RemoveSongFromPlaylist(user, songId, playlistId, replyTo) =>
        playlists.get(playlistId) match {
          case Some(playlist) =>
            val updatedPlaylist = playlist.removeSong(songId)
            // Update the shared playlist state
            playlists += (playlistId -> updatedPlaylist)

            // Log and broadcast update
            context.log.info(s"Song with ID '$songId' removed from playlist '${playlist.name}' by user '$user'. Total songs: ${updatedPlaylist.songs.size}")
            context.system.eventStream ! EventStream.Publish(PlaylistUpdated(updatedPlaylist))

            replyTo ! true
          case None =>
            context.log.error(s"Failed to remove song. Playlist '$playlistId' not found.")
            replyTo ! false
        }
        Behaviors.same

      case msg =>
        // Handle unknown messages gracefully
        context.log.warn(s"Unknown message received: $msg.")
        Behaviors.same
    }
  }
}
