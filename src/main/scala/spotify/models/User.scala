package spotify.models

case class User(
                 username: String,
                 playlists: List[Playlist] = List.empty,
                 contributedSongs: List[String] = List.empty // Tracks songs added by this user
               )
