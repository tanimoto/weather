package actors

import akka.actor._

import play.api.Play.current

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws._

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple
import play.api.db.slick.Config.driver.simple._

import scala.language.postfixOps
import scala.concurrent.duration._

import org.joda.time.DateTime
import scala.util.Random

import models._

object WeatherProtocol {
  case object Start
}

object WeatherActors {
  val system = ActorSystem("weather")
}
