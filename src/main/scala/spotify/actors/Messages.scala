package spotify.actors

import akka.actor.ActorRef
import spotify.models.{Playlist, Song, User}

object Messages {
  sealed trait Command

  // Song Database Messages
  case class SearchSongs(query: String) extends Command // Search songs by query
  case class SearchSongsWithGenre(query: String, genre: String) extends Command // Search songs with genre filter
  case class GetSongById(id: String, replyTo: ActorRef) extends Command          // Get a song by its ID
  case object GetGenres extends Command                                          // Fetch available genres

  // Playlist Messages
  case class AddSongToPlaylist(user: String, song: Song, playlistId: String, replyTo: ActorRef) extends Command
  case class RemoveSongFromPlaylist(user: String, songId: String, replyTo: ActorRef) extends Command
  case class AddContributor(username: String, playlistId: String, replyTo: ActorRef) extends Command
  case class GetPlaylist(replyTo: ActorRef) extends Command

  // Playback Messages
  case class PlaySong(song: Song) extends Command                                // Play a specific song
  case object StopSong extends Command                                           // Stop the currently playing song
  case object Pause extends Command                                              // Pause the playback
  case object Next extends Command                                               // Skip to the next song
  case object Previous extends Command                                           // Go to the previous song

  // Queue Management Messages
  case class AddToQueue(song: Song, replyTo: ActorRef) extends Command           // Add a song to the playback queue
  case class GetQueue(replyTo: ActorRef) extends Command                         // Retrieve the current playback queue
  case object SkipToNext extends Command                                         // Skip to the next song in the queue

  // User Management Messages
  case class LoginUser(user: User, replyTo: ActorRef) extends Command
  case class LogoutUser(username: String, replyTo: ActorRef) extends Command
}

