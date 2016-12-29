package daos

import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.GeoSnap
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Completed, Document, MongoCollection}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import services.Mongo

import scala.concurrent.Future

@ImplementedBy(classOf[MongoGeoSnapDao])
trait GeoSnapDao {
  def create(geo: GeoSnap): Future[Completed]

  def receive(id: String, ts: Long, margin: Int): Future[JsValue]

  def receive(id: String, from: Long, to: Long): Future[Seq[JsValue]]
}

@Singleton
class MongoGeoSnapDao @Inject()(mongo: Mongo) extends GeoSnapDao {
  private val geosnaps: MongoCollection[Document] = mongo.db getCollection "geosnaps"

  override def create(geo: GeoSnap): Future[Completed] = geosnaps insertOne Document(Json toJson geo toString()) head()

  // TODO: cut out mongo id, convert long back to string
  override def receive(id: String, ts: Long, margin: Int): Future[JsValue] =
    geosnaps find Filters.and(
      Filters.eq(GeoSnap.ID, id),
      Filters.gte(GeoSnap.TIME, ts - margin * 500),
      Filters.lte(GeoSnap.TIME, ts + margin * 500)) head() map (Json parse _.toJson) // TODO: add sorting by exactness

  override def receive(id: String, from: Long, to: Long): Future[Seq[JsValue]] =
    geosnaps find Filters.and(
      Filters eq(GeoSnap.ID, id),
      Filters lte(GeoSnap.TIME, to),
      Filters gte(GeoSnap.TIME, from)) map (Json parse _.toJson) toFuture()
}
