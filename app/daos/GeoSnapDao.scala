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
  private val geoDocToJson: Document => String =
    doc => doc - "_id" + ("time" -> doc.get("time").get.asInt64.getValue.toString) toJson()

  override def create(geo: GeoSnap): Future[Completed] = geosnaps insertOne Document(Json toJson geo toString) head()

  override def receive(id: String, ts: Long, margin: Int): Future[JsValue] =
    geosnaps find Filters.and(
      Filters.eq(GeoSnap.ID, id),
      Filters.gte(GeoSnap.TIME, ts - margin * 500),
      Filters.lte(GeoSnap.TIME, ts + margin * 500)) toFuture() map { seq =>
      Json parse (seq.sortBy(doc => math.abs(doc.get(GeoSnap.TIME).get.asNumber.longValue - ts)).headOption match {
        case None => throw new IllegalStateException
        case Some(d) => geoDocToJson(d)
      })
    }

  override def receive(id: String, from: Long, to: Long): Future[Seq[JsValue]] =
    geosnaps find Filters.and(
      Filters eq(GeoSnap.ID, id),
      Filters lte(GeoSnap.TIME, to),
      Filters gte(GeoSnap.TIME, from)) toFuture() map {
      _ sortBy (_.get(GeoSnap.TIME).get.asNumber.longValue) map (Json parse geoDocToJson(_))
    }
}
