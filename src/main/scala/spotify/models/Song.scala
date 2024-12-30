package spotify.models

import javafx.beans.property.{SimpleStringProperty, StringProperty}

case class Song(
                 id: String,
                 title: String,
                 artist: String,
                 genre: String,
                 duration: Int,
                 filePath: String,
                 contributor: Option[String] = None
               ) {
  // JavaFX properties for TableView binding
  def titleProperty(): StringProperty = new SimpleStringProperty(title)
  def artistProperty(): StringProperty = new SimpleStringProperty(artist)
  def genreProperty(): StringProperty = new SimpleStringProperty(genre)
  def durationProperty(): StringProperty = new SimpleStringProperty(duration.toString)
  def contributorProperty(): StringProperty = new SimpleStringProperty(contributor.getOrElse("Unknown"))
}
