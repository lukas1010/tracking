package controllers

import com.google.inject.Inject
import daos.AssetDao
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.Future

class AssetsController @Inject()(assetDao: AssetDao) extends Controller {
  def add(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    assetDao exists id flatMap
      (if (_) Future(BadRequest("ID already exists.")) else assetDao create(id, request.body) map (_ => Accepted))
  }

  def update(id: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    assetDao exists id flatMap
      (if (_) assetDao update(id, request.body) map (_ => Accepted) else Future(NotFound("ID not found.")))
  }

  def read(id: String): Action[AnyContent] =
    Action async (assetDao receive id map (Ok(_)) recoverWith { case _: IllegalStateException => Future(NotFound) })

  def delete(id: String): Action[AnyContent] = Action async (assetDao delete id map (_ => Ok))
}
