package spotify.models

import java.io.Serializable

case class Playlist(
                     id: String,
                     name: String,
                     songs: List[Song] = List.empty,
                     contributors: Set[String] = Set.empty // Changed to Set for better management
                   ) extends Serializable {
  def addSong(song: Song): Playlist = copy(songs = songs :+ song)
  def removeSong(songId: String): Playlist = copy(songs = songs.filterNot(_.id == songId))
  def addContributor(contributor: String): Playlist = copy(contributors = contributors + contributor) // Avoid duplicates
}
