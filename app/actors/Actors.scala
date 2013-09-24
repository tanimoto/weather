package actors

import akka.actor._

import play.api.Play.current

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple
import play.api.db.slick.Config.driver.simple._

import scala.concurrent.duration._

import org.joda.time.DateTime
import com.github.tototoshi.csv._

import models._
import utils.TMY
import utils.IsdFtp

object WeatherProtocol {
  case object Start
  case object Stop
}

object WeatherActors {
  val system = ActorSystem("weather")

  val tmyStation = system.actorOf(Props(new TmyStationActor()))
  val tmyWeather = system.actorOf(Props(new TmyWeatherActor()))

  val isdStation = system.actorOf(Props(new IsdStationActor()))
  val isdWeather = system.actorOf(Props(new IsdWeatherActor()))
}

//----------------------------------------------------------------------
class TmyStationActor extends Actor with ActorLogging {
  import WeatherProtocol._

  def receive = {
    case Start => setup()
  }

  def setup() = {
    DB.withSession { implicit session: simple.Session =>
      if (Query(TmyStationDb).to[Seq].isEmpty) {
        import com.github.tototoshi.csv._

        val file = new java.io.File("data/TMY3_StationsMeta.csv")
        val reader = CSVReader.open(file)
        val csv = reader.all().drop(1)
        reader.close()

        val data = csv.map { col =>
          TmyStation(
            usaf = col(0),
            name = col(1),
            state = col(2),
            latitude = col(3).toDouble,
            longitude = col(4).toDouble,
            timeOffset = col(5).toDouble,
            elevation = col(6).toDouble,
            stationClass = col(7),
            pool = col(8).toLong
          )
        }
        TmyStationDb.insertAll(data : _*)
      }
    }
  }
}

class TmyWeatherActor extends Actor with ActorLogging {
  import WeatherProtocol._

  def receive = {
    case station: String =>
      download(station)
  }

  def download(station: String) = {
    DB.withSession { implicit session: simple.Session =>

      val url = s"http://rredc.nrel.gov/solar/old_data/nsrdb/1991-2005/data/tmy3/${station}TY.csv"
      val fut = WS.url(url).get
      fut.map {
        case resp: Response =>
          if (resp.status == 200) {
            val tmy = TMY.parseTmy(station, resp.body)
            TmyWeatherDb.insertAll(tmy : _*)
          }
      }
    }
  }
}

//----------------------------------------------------------------------
class IsdStationActor extends Actor with ActorLogging {
  import WeatherProtocol._

  def receive = {
    case Start => setup()
  }

  def setup() = {
    DB.withSession { implicit session: simple.Session =>
      if (Query(IsdStationDb).to[Seq].isEmpty) {
        def parse(str: String): IsdStation = {
          val list = str.split("/").last.split("-")
          IsdStation(
            usaf = list(0),
            wban = list(1)
          )
        }

        // Download list of files
        val year = 2012
        val noaa = new IsdFtp()
        val data = noaa.listYear(year).map(parse)
        noaa.disconnect()

        IsdStationDb.insertAll(data : _*)
      }
    }
  }
}

class IsdWeatherActor extends Actor with ActorLogging {
  import WeatherProtocol._

  def receive = {
    case station: String =>
      download(station)
  }

  def download(station: String) = {
    DB.withSession { implicit session: simple.Session =>
      val stations = Query(IsdStationDb)
        .filter(s => s.usaf === station || s.wban === station).to[Seq]
      for {
        stat <- stations
      } {
        val year = 2012

        // Download from FTP
        val noaa = new IsdFtp()
        val data = noaa.getWeather(stat.usaf, stat.wban, year).getOrElse(Seq())
        noaa.disconnect()

        // Insert into database
        IsdWeatherDb.insertAll(data : _*)
      }
    }
  }
}

