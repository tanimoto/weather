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

  implicit val IsdStationReads = Json.reads[IsdStation]
  implicit val IsdStationWrites = Json.writes[IsdStation]
  implicit val IsdStationFormat = Json.format[IsdStation]

  implicit val IsdWeatherReads = Json.reads[IsdWeather]
  implicit val IsdWeatherWrites = Json.writes[IsdWeather]
  implicit val IsdWeatherFormat = Json.format[IsdWeather]
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

//------------------------------------------------------------------------
case class IsdStation(
  usaf: String,
  wban: String
)

object IsdStationDb extends Table[IsdStation]("IsdStation") {
  def usaf = column[String]("usaf")
  def wban = column[String]("wban")

  def * = usaf ~ wban <> (IsdStation, IsdStation.unapply _)

  def pk = primaryKey("pk_IsdStation", (usaf, wban))
}

case class IsdWeather(
  usaf: String,
  wban: String,
  datetime: DateTime,
  tempDry: Double,
  tempDew: Double
)

object IsdWeatherDb extends Table[IsdWeather]("IsdWeather") {
  def usaf = column[String]("usaf")
  def wban = column[String]("wban")
  def datetime = column[DateTime]("datetime")
  def tempDry = column[Double]("tempDry")
  def tempDew = column[Double]("tempDew")

  def * = usaf ~ wban ~ datetime ~ tempDry ~ tempDew <>
    (IsdWeather, IsdWeather.unapply _)

  // def pk = primaryKey("pk_IsdWeather", (usaf, wban, datetime))
  def idx_usaf = index("idx_IsdWeather_usaf", usaf)
  def idx_wban = index("idx_IsdWeather_wban", wban)
  def fk_station = foreignKey("fk_IsdStation", (usaf, wban),
    IsdStationDb) (k => (k.usaf, k.wban))
}
