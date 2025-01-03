package spotify.actors

import akka.actor.{Actor, Props}
import akka.event.EventStream
import spotify.models.{Playlist, Song}
import spotify.actors.Messages._

class PlaylistActor extends Actor {
  // Map to store playlists by username
  private var playlists: Map[String, Playlist] = Map(
    "default" -> Playlist("1", "Shared Playlist") // Default playlist
  )
  private var contributors: Set[String] = Set.empty
  private var songQueue: List[Song] = List.empty
  private val eventStream: EventStream = context.system.eventStream // EventStream for real-time updates

  override def receive: Receive = {
    case GetPlaylist(username, replyTo) =>
      // Fetch the playlist for the given username
      val playlist = playlists.getOrElse(username, playlists("default")) // Use default if no user-specific playlist exists
      context.system.log.info(s"Received GetPlaylist request for user '$username'. Sending playlist: ${playlist.name}")
      replyTo ! playlist

    case AddSongToPlaylist(user, song, playlistId, replyTo) =>
      val currentPlaylist = playlists.getOrElse(user, playlists("default")) // Fetch user's playlist or default
      if (contributors.contains(user) || user == "default") {
        val updatedPlaylist = currentPlaylist.addSong(song.copy(contributor = Some(user)))
        playlists += (user -> updatedPlaylist) // Update the user's playlist
        replyTo ! true
        context.system.log.info(s"Song '${song.title}' added to playlist '${currentPlaylist.name}' by user '$user'.")
        eventStream.publish(PlaylistUpdated(updatedPlaylist)) // Notify clients of the update
      } else {
        replyTo ! false
        context.system.log.warning(s"User '$user' is not authorized to add songs to playlist '$playlistId'.")
      }

    case AddContributor(username, playlistId, replyTo) =>
      contributors += username
      replyTo ! true
      context.system.log.info(s"User '$username' added as a contributor to playlist '$playlistId'.")

    case PlaylistUpdated(playlist) =>
      context.system.log.info(s"Playlist '${playlist.name}' updated. Broadcasting to subscribers.")
      eventStream.publish(PlaylistUpdated(playlist)) // Publish the playlist update to all listeners

    case _ =>
      context.system.log.warning("Received an unknown message.")
  }
}

object PlaylistActor {
  def props(): Props = Props[PlaylistActor]
}
