package controllers

import com.google.inject.Inject
import daos.GeoSnapDao
import models.GeoSnap
import org.joda.time.DateTime
import org.mongodb.scala.Completed
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Controller, Result}

import scala.concurrent.Future

class TrackingController @Inject()(geoSnapDao: GeoSnapDao) extends Controller {
  private val invJson: (JsError) => Future[Result] = { e: JsError =>
    Future(BadRequest(Json toJson (e.errors map { case (a, b) => Json obj a.toString -> b.mkString("; ") })))
  }
  private val invMsg: (AnyRef, String) => Future[Result] = (e, m) => Future(BadRequest(Json obj m -> e.toString))
  private val recMsg: PartialFunction[Throwable, Future[Result]] = {
    case e: IllegalStateException => Future(NotFound(Json obj "Empty database return." -> e.toString))
  }

  def add(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    try {
      request.body.validate[GeoSnap] match {
        case e: JsError => invJson(e)
        case s: JsSuccess[GeoSnap] => geoSnapDao create s.get map { _: Completed => Accepted(Json toJson s.get) }
      }
    } catch {
      case a: AssertionError => invMsg(a, "Invalid data")
      case i: IllegalArgumentException => invMsg(i, "Invalid data")
    }
  }

  def position(
                ids: Seq[String],
                lat1: Double,
                lng1: Double,
                lat2: Double,
                lng2: Double,
                time: String,
                window: Int): Action[AnyContent] = Action.async {
    try {
      val Seq(lat_left, lat_right) = Seq(
        if (lat1 >= -180 && lat1 <= 180) lat1 else -180,
        if (lat2 >= -180 && lat2 <= 180) lat2 else 180).sorted
      val Seq(lng_bottom, lng_top) = Seq(
        if (lng1 >= -90 && lng1 <= 90) lng1 else -90,
        if (lng2 >= -90 && lng2 <= 90) lng2 else 90).sorted
      val ts = if (time == "") DateTime.now() getMillis() else DateTime parse time getMillis()
      val w = if (window > 0) window else 60
      geoSnapDao receive(ids, lat_left, lat_right, lng_bottom, lng_top, ts, w) map
        (seq => Ok(Json toJson seq)) recoverWith recMsg
    } catch {
      case i: IllegalArgumentException => invMsg(i, "Invalid time string format.")
    }
  }

  def route(id: String, start: String, finish: String): Action[AnyContent] = Action.async {
    try {
      geoSnapDao receive(id, DateTime parse start getMillis, DateTime parse finish getMillis) map
        (s => Ok(Json toJson s)) recoverWith recMsg
    } catch {
      case i: IllegalArgumentException => invMsg(i, "Invalid request")
    }
  }
}
