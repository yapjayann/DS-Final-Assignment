package spotify.actors

import akka.actor.{Actor, Props}
import spotify.actors.Messages._
import spotify.models.Song

class SongDatabaseActor extends Actor {

  // Songs database (mock data)
  private val songs: List[Song] = List(
    Song("1", "Don't", "Bryson Tiller", "R&B", 38, getClass.getResource("/MP3/Don't - Bryson Tiller.mp3").toString),
    Song("2", "Dreamin", "PartyNextDoor", "R&B", 27, getClass.getResource("/MP3/Dreamin - PartyNextDoor.mp3").toString),
    Song("3", "Make It 2 The Morning", "PartyNextDoor", "R&B", 38, getClass.getResource("/MP3/Makeit2theMorning - PartyNextDoor.mp3").toString),
    Song("4", "H.S.K.T", "LeeHi", "K-Pop", 35, getClass.getResource("/MP3/LeeHi H.S.K.T.mp3").toString),
    Song("5", "Cupid", "FIFTY FIFTY", "K-Pop", 34, getClass.getResource("/MP3/Cupid - FIFTY FIFTY.mp3").toString),
    Song("6", "Magnetic", "ILLIT", "K-Pop", 55, getClass.getResource("/MP3/Magnetic - ILLIT.mp3").toString),
    Song("7", "Soft Spot", "Keshi", "Pop", 35, getClass.getResource("/MP3/Soft Spot - Keshi.mp3").toString),
    Song("8", "Taste", "Sabrina Carpenter", "Pop", 35, getClass.getResource("/MP3/Taste - Sabrina Carpenter.mp3").toString),
    Song("9", "Bow Down", "i Prevail", "Rock/Metal", 41, getClass.getResource("/MP3/Bow Down - i Prevail.mp3").toString),
    Song("10", "ぴぽぴぽ Pipo", "Serani Poji", "J-Pop", 34, getClass.getResource("/MP3/ぴぽぴぽ Pipo Pipo - Serani Poji.mp3").toString),
    Song("11", "ラブソング", "Natori", "J-Pop", 25, getClass.getResource("/MP3/ラブソング - Natori.mp3").toString),
    Song("12", "Overdose", "Natori", "J-Pop", 49, getClass.getResource("/MP3/Overdose - Natori.mp3").toString)
  )

  override def preStart(): Unit = {
    context.system.log.info("SongDatabaseActor started successfully.")
  }

  override def postStop(): Unit = {
    context.system.log.info("SongDatabaseActor has been stopped.")
  }

  override def receive: Receive = {
    case SearchSongs(query) =>
      context.system.log.info(s"Received SearchSongs request with query: '$query'")
      val results = songs.filter(song =>
        song.title.toLowerCase.contains(query.toLowerCase) ||
          song.artist.toLowerCase.contains(query.toLowerCase) ||
          song.genre.toLowerCase.contains(query.toLowerCase)
      )
      sender() ! results
      context.system.log.info(s"Search query: '$query'. Found ${results.size} results.")

    case SearchSongsWithGenre(query, genre) =>
      context.system.log.info(s"Received SearchSongsWithGenre request with query: '$query' and genre: '$genre'")
      val results = songs.filter(song =>
        (song.title.toLowerCase.contains(query.toLowerCase) ||
          song.artist.toLowerCase.contains(query.toLowerCase)) &&
          song.genre.toLowerCase == genre.toLowerCase
      )
      sender() ! results
      context.system.log.info(s"Search query: '$query', genre: '$genre'. Found ${results.size} results.")

    case GetSongById(id, replyTo) =>
      context.system.log.info(s"Received GetSongById request for song ID: '$id'")
      val song = songs.find(_.id == id)
      song match {
        case Some(foundSong) =>
          replyTo ! foundSong
          context.system.log.info(s"Song with ID '$id' found: '${foundSong.title}' by '${foundSong.artist}'.")
        case None =>
          replyTo ! None
          context.system.log.warning(s"Song with ID '$id' not found.")
      }

    case GetGenres =>
      context.system.log.info("Received GetGenres request.")
      val genres = songs.map(_.genre).distinct
      sender() ! genres
      context.system.log.info(s"Available genres retrieved: ${genres.mkString(", ")}")

    case _ =>
      context.system.log.warning("Received an unknown message in SongDatabaseActor.")
  }
}

object SongDatabaseActor {
  def props(): Props = Props[SongDatabaseActor]
}
