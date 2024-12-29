package actors

import spotify.models.Song

object Messages {
  // Song Database Messages
  case class SearchSongs(query: String) // Search songs by query
  case class SearchSongsWithGenre(query: String, genre: String) // Search songs with genre filter
  case class GetSongById(id: String) // Get a song by its ID
  case object GetGenres // Fetch available genres

  // Playlist Messages
  case class AddSongToPlaylist(user: String, song: Song) // Add a song to a user's playlist
  case class RemoveSongFromPlaylist(user: String, songId: String) // Remove song from a playlist
  case class AddContributor(username: String) // Add a contributor to the playlist
  case object GetPlaylist // Get the current playlist of the user

  // Playback Messages
  case class PlaySong(song: Song)  // Play a specific song
  case object StopSong            // Stop the currently playing song
  case object Pause               // Pause the playback
  case object Next                // Skip to the next song
  case object Previous            // Go to the previous song

  // User Management Messages
  case class LoginUser(username: String) // User login message
  case class LogoutUser(username: String) // User logout message
}
