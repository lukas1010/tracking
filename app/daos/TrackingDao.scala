package daos

import models.GeoSnap
import org.mongodb.scala.Completed
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.Future

trait TrackingDao {
  def create(geo: GeoSnap): Future[Completed]

  def receive(id: String, ts: String, margin: Int = 1): Future[JsValue]

  def find(id: String, from: String, to: String): Future[Seq[JsObject]]
}
