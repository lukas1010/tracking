package controllers

import com.google.inject.Inject
import daos.AssetDao
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller}

import scala.concurrent.Future

class AssetsController @Inject()(assetDao: AssetDao, configuration: Configuration) extends Controller {
  def update(token: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    if (token == (configuration.underlying getString "token.write")) {
      val id = request.body.as[JsObject].fields.head._1
      request.body.as[JsObject].fields.head._2 match {
        case o: JsObject => assetDao exists id flatMap {
          if (_) assetDao update(id, o) map (_ => Ok("Updated.")) else assetDao create(id, o) map (_ => Ok("Created."))
        }
        case _ => Future(BadRequest("Invalid request body."))
      }
    } else Future(Unauthorized)
  }

  def read(id: Option[String], token: String): Action[AnyContent] = Action async {
    if (token == (configuration.underlying getString "token.read")) {
      id match {
        case Some(x) => assetDao receive x map (Ok(_)) recoverWith { case _: IllegalStateException => Future(NotFound) }
        case None => assetDao list() map (Ok(_))
      }
    } else Future(Unauthorized)
  }

  def delete(id: String, token: String): Action[AnyContent] = Action async {
    if (token == (configuration.underlying getString "token.delete")) assetDao delete id map (r => Ok(r.toString))
    else Future(Unauthorized)
  }
}
