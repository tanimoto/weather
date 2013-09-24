package controllers

import akka.actor._

import play.api._
import play.api.mvc._

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._

import play.api.Play.current

import play.api.db.slick._
import play.api.db.slick.Config.driver.simple
import play.api.db.slick.Config.driver.simple._

import scala.concurrent.Future
import scala.concurrent.duration._

import actors._
import models._
import models.Implicits._
import utils.TMY

object Application extends Controller {

  def index = Action {
    Ok("Index")
  }

  def todo(args: String*) = TODO

  //--------------------------------------------------------------------------
  def getTypicalStations = Action.async {
    Future[SimpleResult] {
      DB.withSession { implicit session: simple.Session =>
        val data = Query(TmyStationDb).to[Seq]
        val json = Json.toJson(data)
        Ok(json)
      }
    }
  }

  def getTypicalWeather(station: String) = Action.async {
    Future[SimpleResult] {
      DB.withSession { implicit session: simple.Session =>
        val data = Query(TmyWeatherDb).filter(_.usaf === station).to[Seq]
        // Do we already have the data?
        if (!data.isEmpty) {
          val json = Json.toJson(data)
          Ok(json)
        } else {
          WeatherActors.tmyWeather ! station
          Ok("requested")
        }
      }
    }
  }

  //--------------------------------------------------------------------------
  def getHistoricalStations = Action.async {
    Future[SimpleResult] {
      DB.withSession { implicit session: simple.Session =>
        val data = Query(IsdStationDb).to[Seq]
        val json = Json.toJson(data)
        Ok(json)
      }
    }
  }

  def getHistoricalWeather(station: String) = Action.async {
    Future[SimpleResult] {
      DB.withSession { implicit session: simple.Session =>
        val data = Query(IsdWeatherDb).filter(_.usaf === station).to[Seq]
        if (!data.isEmpty) {
          val json = Json.toJson(data)
          Ok(json)
        } else {
          WeatherActors.isdWeather ! station
          Ok("requested")
        }
      }
    }
  }
}
