package spotify.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import spotify.models.Song
import spotify.actors.Messages._

object SongDatabaseActor {
  def apply(): Behavior[Command] = Behaviors.setup { context =>
    val songs: List[Song] = List(
      Song("1", "Don't", "Bryson Tiller", "R&B", 38, getClass.getResource("/MP3/Don't - Bryson Tiller.mp3").toString),
      Song("2", "Dreamin", "PartyNextDoor", "R&B", 27, getClass.getResource("/MP3/Dreamin - PartyNextDoor.mp3").toString),
      Song("3", "Make It 2 The Morning", "PartyNextDoor", "R&B", 38, getClass.getResource("/MP3/Makeit2theMorning - PartyNextDoor.mp3").toString),
      Song("4", "H.S.K.T", "LeeHi", "K-Pop", 35, getClass.getResource("/MP3/LeeHi H.S.K.T.mp3").toString),
      Song("5", "Cupid", "FIFTY FIFTY", "K-Pop", 34, getClass.getResource("/MP3/Cupid - FIFTY FIFTY.mp3").toString),
      Song("6", "Magnetic", "ILLIT", "K-Pop", 55, getClass.getResource("/MP3/Magnetic - ILLIT.mp3").toString),
      Song("6", "Soft Spot", "Keshi", "Pop", 35, getClass.getResource("/MP3/Soft Spot - Keshi.mp3").toString),
      Song("6", "Taste", "Sabrina Carpenter", "Pop", 35, getClass.getResource("/MP3/Taste - Sabrina Carpenter.mp3").toString),
      Song("7", "Bow Down", "i Prevail", "Rock/Metal", 41, getClass.getResource("/MP3/Bow Down - i Prevail.mp3").toString),
      Song("8", "ぴぽぴぽ Pipo", "Serani Poji", "J-Pop", 34, getClass.getResource("/MP3/ぴぽぴぽ Pipo Pipo - Serani Poji.mp3").toString),
      Song("9", "ラブソング", "Natori", "J-Pop", 25, getClass.getResource("/MP3/ラブソング - Natori.mp3").toString),
      Song("10", "Overdose", "Natori", "J-Pop", 49, getClass.getResource("/MP3/Overdose - Natori.mp3").toString)
    )

    Behaviors.receiveMessage {
      case SearchSongs(query, replyTo) =>
        val results = songs.filter(song =>
          song.title.toLowerCase.contains(query.toLowerCase) ||
            song.artist.toLowerCase.contains(query.toLowerCase) ||
            song.genre.toLowerCase.contains(query.toLowerCase)
        )
        context.log.info(s"Found ${results.size} songs matching query '$query'")
        replyTo ! results
        Behaviors.same

      case SearchSongsWithGenre(query, genre, replyTo) =>
        val results = songs.filter(song =>
          (song.title.toLowerCase.contains(query.toLowerCase) ||
            song.artist.toLowerCase.contains(query.toLowerCase)) &&
            song.genre.toLowerCase == genre.toLowerCase
        )
        context.log.info(s"Found ${results.size} songs matching query '$query' and genre '$genre'")
        replyTo ! results
        Behaviors.same

      case GetSongById(id, replyTo) =>
        val song = songs.find(_.id == id)
        song match {
          case Some(s) =>
            context.log.info(s"Found song with ID '$id'")
            replyTo ! Some(s)
          case None =>
            context.log.info(s"Song with ID '$id' not found")
            replyTo ! None
        }
        Behaviors.same

      case GetGenres(replyTo) =>
        val genres = songs.map(_.genre).distinct
        context.log.info(s"Retrieved ${genres.size} unique genres")
        replyTo ! genres
        Behaviors.same

      case msg =>
        context.log.info(s"Received unknown message: $msg")
        Behaviors.same
    }
  }
}