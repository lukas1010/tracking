package controllers

import com.google.inject.Inject
import daos.GeoSnapDao
import models.GeoSnap
import org.mongodb.scala.Completed
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Controller, Result}

import scala.concurrent.Future

class TrackingController @Inject()(geoSnapDao: GeoSnapDao) extends Controller {
  private val invalidMsg: (JsError) => Future[Result] = { e: JsError =>
    Future(BadRequest(Json toJson (e.errors map { case (a, b) => Json obj a.toString -> b.mkString("; ") })))
  }
  private val recMsg: PartialFunction[Throwable, Future[Result]] = {
    case e: IllegalStateException => Future(NotFound(Json obj "Empty database return." -> e.toString))
  }

  def add(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    request.body.validate[GeoSnap] match {
      case e: JsError => invalidMsg(e)
      case s: JsSuccess[GeoSnap] => geoSnapDao create s.get map { _: Completed => Accepted(Json toJson s.get) }
    }
  }

  def track(id: String, time: Long, window: Int): Action[AnyContent] =
    Action.async(geoSnapDao receive(id, time, window) map (Ok(_)) recoverWith recMsg)

  def route(id: String, start: Long, finish: Long): Action[AnyContent] =
    Action.async(geoSnapDao receive(id, start, finish) map { seq => Ok(Json toJson seq) } recoverWith recMsg)
}
