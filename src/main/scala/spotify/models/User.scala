package spotify.models

import java.io.Serializable

case class User(
                 username: String,
                 playlists: List[Playlist] = List.empty,
                 contributedSongs: List[String] = List.empty // Tracks songs added by this user
               ) extends Serializable {
  def addPlaylist(playlist: Playlist): User = copy(playlists = playlists :+ playlist)
  def removePlaylist(playlistId: String): User = copy(playlists = playlists.filterNot(_.id == playlistId))
  def addContributedSong(songId: String): User = copy(contributedSongs = contributedSongs :+ songId)
}
