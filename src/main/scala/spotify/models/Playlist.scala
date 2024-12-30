package spotify.models

case class Playlist(
                     id: String,
                     name: String,
                     songs: List[Song] = List.empty,
                     contributors: List[String] = List.empty
                   ) {
  def addSong(song: Song): Playlist = copy(songs = songs :+ song)
  def removeSong(songId: String): Playlist = copy(songs = songs.filterNot(_.id == songId))
  def addContributor(contributor: String): Playlist = copy(contributors = contributors :+ contributor)
}
