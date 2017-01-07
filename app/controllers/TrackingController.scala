package controllers

import com.google.inject.Inject
import daos.GeoSnapDao
import models.GeoSnap
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller, Result}

import scala.concurrent.Future

class TrackingController @Inject()(geoSnapDao: GeoSnapDao, configuration: Configuration) extends Controller {
  private val invJson: (JsError) => Future[Result] = { e: JsError =>
    Future(BadRequest(Json toJson (e.errors map { case (a, b) => Json obj a.toString -> b.mkString("; ") })))
  }
  private val invMsg: (AnyRef, String) => Future[Result] = (e, m) => Future(BadRequest(Json obj m -> e.toString))
  private val recMsg: PartialFunction[Throwable, Future[Result]] = {
    case e: IllegalStateException => Future(NotFound(Json obj "Empty database return." -> e.toString))
  }

  def add(id: String, token: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    if (token == (configuration.underlying getString "token.write")) {
      try {
        val ts = request.body.as[JsObject].fields.head._1
        (request.body \ ts).get validate GeoSnap.geoSnapFormat match {
          case e: JsError => invJson(e)
          case s: JsSuccess[GeoSnap] =>
            geoSnapDao create(id, DateTime parse ts getMillis, s.get copy (timestamp = Some(ts))) map
              (r => Accepted(r.toString))
        }
      } catch {
        case a: AssertionError => invMsg(a, "Invalid data")
        case i: IllegalArgumentException => invMsg(i, "Invalid data")
      }
    } else Future(Unauthorized)
  }

  def position(
                ids: Seq[String],
                lat1: Option[Double],
                long1: Option[Double],
                lat2: Option[Double],
                long2: Option[Double],
                time: Option[String],
                window: Option[Int],
                route: Boolean,
                token: String): Action[AnyContent] = Action.async {
    if (token == (configuration.underlying getString "token.read")) {
      try {
        def chk(x: Option[Double], z: Int): Boolean = x match {
          case Some(d) if d >= -z && d <= z => true
          case None => false
          case Some(d) => throw new IllegalArgumentException(s"Invalid coordinate: $d")
        }

        val geo = if (chk(lat1, 180) && chk(lat2, 180) && chk(long1, 90) && chk(long2, 90) && long1.get > long2.get)
          Some((lat1.get, lat2.get, long2.get, long1.get)) else None
        val now = DateTime.now() getMillis()
        val ts = if (time.isEmpty) now else math.min(DateTime parse time.get getMillis(), now)
        val w = if (window.isDefined && window.get > 0) window.get else 60
        (if (route) geoSnapDao receiveRoute(ids, geo, ts, w) else geoSnapDao receiveLast(ids, geo, ts, w)) map {
          seq => Ok(Json toJson seq)
        }
      } catch {
        case i: IllegalArgumentException => invMsg(i, "Invalid parameters.")
      }
    } else Future(Unauthorized)
  }
}
