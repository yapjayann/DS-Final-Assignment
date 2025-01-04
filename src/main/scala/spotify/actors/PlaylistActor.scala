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
    // Initialize the playlists map with a shared playlist and store it with both a default key and the playlist ID
    val sharedPlaylist = Playlist("1", "Shared Playlist")
    var playlists: Map[String, Playlist] = Map(
      "default" -> sharedPlaylist,
      "1" -> sharedPlaylist  // Add the playlist with its ID as a key
    )

    Behaviors.receiveMessage {
      case GetPlaylist(username, replyTo) =>
        val playlist = playlists.getOrElse(username, playlists("default"))
        context.log.info(s"Sending playlist '${playlist.name}' to user '$username'")
        replyTo ! playlist
        Behaviors.same

      case AddSongToPlaylist(user, song, playlistId, replyTo) =>
        playlists.get(playlistId) match {
          case Some(playlist) =>
            val updatedSong = song.copy(contributor = Some(user))
            val updatedPlaylist = playlist.addSong(updatedSong)
            // Update both the ID-based and username-based entries if they exist
            playlists += (playlistId -> updatedPlaylist)
            if (playlists.contains("default") && playlistId == "1") {
              playlists += ("default" -> updatedPlaylist)
            }
            context.log.info(s"Song '${song.title}' added to playlist '${playlist.name}' by user '$user'")
            context.system.eventStream.tell(EventStream.Publish(PlaylistUpdated(updatedPlaylist)))
            replyTo ! true
          case None =>
            context.log.error(s"Playlist '$playlistId' not found")
            replyTo ! false
        }
        Behaviors.same

      case RemoveSongFromPlaylist(user, songId, playlistId, replyTo) =>
        playlists.get(playlistId) match {
          case Some(playlist) =>
            val updatedPlaylist = playlist.removeSong(songId)
            // Update both the ID-based and username-based entries if they exist
            playlists += (playlistId -> updatedPlaylist)
            if (playlists.contains("default") && playlistId == "1") {
              playlists += ("default" -> updatedPlaylist)
            }
            context.log.info(s"Song '$songId' removed from playlist '$playlistId' by user '$user'")
            context.system.eventStream.tell(EventStream.Publish(PlaylistUpdated(updatedPlaylist)))
            replyTo ! true
          case _ =>
            context.log.info(s"Failed to remove song '$songId' from playlist '$playlistId'")
            replyTo ! false
        }
        Behaviors.same

      case msg =>
        context.log.info(s"Received unknown message: $msg")
        Behaviors.same
    }
  }
}