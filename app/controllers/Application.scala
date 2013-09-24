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

object Application extends Controller {

  def index = Action {
    Ok("Index")
  }

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

}
