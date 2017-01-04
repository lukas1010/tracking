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

  def receive(
               ids: Seq[String],
               lat_left: Double,
               lat_right: Double,
               lng_bottom: Double,
               lng_top: Double,
               time: Long,
               window: Int): Future[Seq[JsValue]]

  def receive(id: String, from: Long, to: Long): Future[Seq[JsValue]]
}

@Singleton
class MongoGeoSnapDao @Inject()(mongo: Mongo) extends GeoSnapDao {
  private val geoSnaps: MongoCollection[Document] = mongo.db getCollection "geosnaps"
  private val geoDocToJson: Document => String = doc => doc - ("_id", GeoSnap.MILLIS) toJson()

  override def create(geo: GeoSnap): Future[Completed] = geoSnaps insertOne Document(Json toJson geo toString) head()

  override def receive(
                        ids: Seq[String],
                        lat_left: Double,
                        lat_right: Double,
                        lng_bottom: Double,
                        lng_top: Double,
                        time: Long,
                        window: Int): Future[Seq[JsValue]] =
    geoSnaps find Filters.and(
      if (ids != Nil) Filters.in(GeoSnap.ID, ids: _*) else Filters.exists(GeoSnap.ID),
      Filters.gte(GeoSnap.LATITUDE, lat_left),
      Filters.lte(GeoSnap.LATITUDE, lat_right),
      Filters.gte(GeoSnap.LONGITUDE, lng_bottom),
      Filters.lte(GeoSnap.LONGITUDE, lng_top),
      Filters.gte(GeoSnap.MILLIS, time - (window * 1000)),
      Filters.lte(GeoSnap.MILLIS, time)) toFuture() map {
      _ groupBy (_ get GeoSnap.ID get) map { case (_, seq) =>
        Json parse geoDocToJson(seq.sortBy(_.get(GeoSnap.MILLIS).get.asNumber.longValue).reverse.head)
      } toSeq
    }

  override def receive(id: String, from: Long, to: Long): Future[Seq[JsValue]] =
    geoSnaps find Filters.and(
      Filters eq(GeoSnap.ID, id),
      Filters lte(GeoSnap.MILLIS, to),
      Filters gte(GeoSnap.MILLIS, from)) toFuture() map {
      _ sortBy (_.get(GeoSnap.MILLIS).get.asNumber.longValue) map (Json parse geoDocToJson(_))
    }
}
