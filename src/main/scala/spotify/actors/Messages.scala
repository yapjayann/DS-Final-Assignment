package spotify.actors

import akka.actor.ActorRef
import spotify.models.{Playlist, Song, User}

object Messages {
  // General trait for all commands
  sealed trait Command

  // Song Database Messages
  case class SearchSongs(query: String) extends Command                      // Search songs by query
  case class SearchSongsWithGenre(query: String, genre: String) extends Command // Search songs with genre filter
  case class GetSongById(id: String, replyTo: ActorRef) extends Command           // Get a song by its ID
  case object GetGenres extends Command                                           // Fetch available genres

  // Playlist Messages
  case class AddSongToPlaylist(user: String, song: Song, playlistId: String, replyTo: ActorRef) extends Command // Add a song to a playlist
  case class RemoveSongFromPlaylist(user: String, songId: String, replyTo: ActorRef) extends Command            // Remove a song from a playlist
  case class AddContributor(username: String, playlistId: String, replyTo: ActorRef) extends Command            // Add a contributor to a playlist
  case class GetPlaylist(username: String, replyTo: ActorRef) extends Command                                   // Retrieve the playlist for a specific user
  case class PlaylistUpdated(playlist: Playlist) extends Command                                               // Notify clients of playlist updates

  // Playback Messages
  case class PlaySong(song: Song) extends Command                                // Play a specific song
  case object Pause extends Command                                              // Pause playback
  case object Next extends Command                                               // Play the next song
  case object Previous extends Command                                           // Play the previous song
  case object StopPlayback extends Command                                       // Stop playback completely
  case object ResumePlayback extends Command                                     // Resume playback

  // Queue Management Messages
  case class AddToQueue(song: Song, replyTo: ActorRef) extends Command           // Add a song to the playback queue
  case class GetQueue(replyTo: ActorRef) extends Command                         // Retrieve the current playback queue
  case object SkipToNext extends Command                                         // Skip to the next song in the queue

  // User Management Messages
  case class LoginUser(user: User, replyTo: ActorRef) extends Command            // Login a user
  case class LogoutUser(username: String, replyTo: ActorRef) extends Command     // Logout a user
  case class GetAllUsers(replyTo: ActorRef) extends Command                      // Fetch all registered users
}
