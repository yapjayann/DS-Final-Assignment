package spotify.actors

import akka.actor.typed.ActorRef
import spotify.models.{Playlist, Song, User}

object Messages {
  // General trait for all commands
  sealed trait Command

  // Song Database Messages
  final case class SearchSongs(query: String, replyTo: ActorRef[List[Song]]) extends Command // Added replyTo
  final case class SearchSongsWithGenre(query: String, genre: String, replyTo: ActorRef[List[Song]]) extends Command // Added replyTo
  final case class GetSongById(id: String, replyTo: ActorRef[Option[Song]]) extends Command
  final case class GetGenres(replyTo: ActorRef[List[String]]) extends Command // Added replyTo

  // Playlist Messages
  final case class AddSongToPlaylist(user: String, song: Song, playlistId: String, replyTo: ActorRef[Boolean]) extends Command
  final case class RemoveSongFromPlaylist(user: String, songId: String, playlistId: String, replyTo: ActorRef[Boolean]) extends Command // Added playlistId
  final case class AddContributor(username: String, playlistId: String, replyTo: ActorRef[Boolean]) extends Command
  final case class GetPlaylist(username: String, replyTo: ActorRef[Playlist]) extends Command
  final case class PlaylistUpdated(playlist: Playlist) extends Command

  // User Management Messages
  final case class LoginUser(user: User, replyTo: ActorRef[Boolean]) extends Command
  final case class LogoutUser(username: String, replyTo: ActorRef[Boolean]) extends Command
  final case class GetAllUsers(replyTo: ActorRef[List[User]]) extends Command
}