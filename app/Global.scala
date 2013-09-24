import play.api._

import play.api.Logger

import akka.actor._

import org.joda.time.DateTimeZone
import org.joda.time.DateTimeZone.UTC

import java.util.TimeZone

import actors._
import models._

object Global extends GlobalSettings {
  override def onStart(app: Application) {
    setup()
  }

  override def onStop(app: Application) {
    WeatherActors.system.shutdown()
  }

  def setup() = {
    import WeatherProtocol._
    WeatherActors

    // Setup TMY Stations
    WeatherActors.tmyStation ! Start
  }
}
