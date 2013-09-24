package models

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.db.slick.Config.driver.simple._
import org.joda.time.DateTime

import com.github.tototoshi.slick.JodaSupport._

object Implicits {
  implicit val TmyStationReads = Json.reads[TmyStation]
  implicit val TmyStationWrites = Json.writes[TmyStation]
  implicit val TmyStationFormat = Json.format[TmyStation]

  implicit val TmyWeatherReads = Json.reads[TmyWeather]
  implicit val TmyWeatherWrites = Json.writes[TmyWeather]
  implicit val TmyWeatherFormat = Json.format[TmyWeather]
}

//------------------------------------------------------------------------
case class TmyStation(
  usaf: String,
  name: String,
  state: String,
  latitude: Double,
  longitude: Double,
  timeOffset: Double,
  elevation: Double,
  stationClass: String,
  pool: Long
)

object TmyStationDb extends Table[TmyStation]("TmyStation") {
  def usaf = column[String]("usaf")
  def name = column[String]("name")
  def state = column[String]("state")
  def latitude = column[Double]("latitude")
  def longitude = column[Double]("longitude")
  def timeOffset = column[Double]("timeOffset")
  def elevation = column[Double]("elevation")
  def stationClass = column[String]("stationClass")
  def pool = column[Long]("pool")

  def * = usaf ~ name ~ state ~ latitude ~ longitude ~
    timeOffset ~ elevation ~ stationClass ~ pool <>
    (TmyStation, TmyStation.unapply _)

  def pk = primaryKey("pk_TmyStation", usaf)
}

case class TmyWeather(
  usaf: String,
  datetime: DateTime,
  tempDry: Double,
  tempDew: Double,
  humi: Double
)

object TmyWeatherDb extends Table[TmyWeather]("TmyWeather") {
  def usaf = column[String]("usaf")
  def datetime = column[DateTime]("datetime")
  def tempDry = column[Double]("tempDry")
  def tempDew = column[Double]("tempDew")
  def humi = column[Double]("humi")

  def * = usaf ~ datetime ~ tempDry ~ tempDew ~ humi <>
    (TmyWeather, TmyWeather.unapply _)

  // def pk = primaryKey("pk_TmyWeather", (usaf, datetime))
  def idx_usaf = index("idx_TmyWeather_usaf", usaf)
  def fk_station = foreignKey("fk_TmyStation", usaf, TmyStationDb)(_.usaf)
}
