package controllers

import com.google.inject.Inject
import daos.AssetDao
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.Future

class AssetsController @Inject()(assetDao: AssetDao) extends Controller {
  def update(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val id = request.body.as[JsObject].fields.head._1
    request.body.as[JsObject].fields.head._2 match {
      case o: JsObject => assetDao exists id flatMap {
        if (_) assetDao update(id, o) map (_ => Ok("Updated.")) else assetDao create(id, o) map (_ => Ok("Created."))
      }
      case _ => Future(BadRequest("Invalid request body."))
    }
  }

  def read(id: Option[String]): Action[AnyContent] = Action async {
      id match {
        case Some(x) => assetDao receive x map (Ok(_)) recoverWith { case _: IllegalStateException => Future(NotFound) }
        case None => assetDao list() map (Ok(_))
      }
    }

  def delete(id: String): Action[AnyContent] = Action async (assetDao delete id map (r => Ok(r.toString)))
}
