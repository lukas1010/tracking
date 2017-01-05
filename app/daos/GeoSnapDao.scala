package daos

import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.GeoSnap
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Completed, Document, MongoCollection}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, JsValue, Json}
import services.Mongo

import scala.concurrent.Future

@ImplementedBy(classOf[MongoGeoSnapDao])
trait GeoSnapDao {
  def create(id: String, mil: Long, geo: GeoSnap): Future[Completed]

  def receiveLast(
                   ids: Seq[String],
                   geo: Option[(Double, Double, Double, Double)],
                   time: Long,
                   window: Long): Future[JsValue]

  def receiveRoute(
                    ids: Seq[String],
                    geo: Option[(Double, Double, Double, Double)],
                    time: Long,
                    window: Long): Future[JsValue]
}

@Singleton
class MongoGeoSnapDao @Inject()(mongo: Mongo) extends GeoSnapDao {
  private val geoSnaps: MongoCollection[Document] = mongo.db getCollection "geosnaps"
  private val idField = "id"
  private val milField = "millis"
  private val locField = "location"

  override def create(id: String, mil: Long, geo: GeoSnap): Future[Completed] =
    geoSnaps insertOne
      Document(s"""{ "$idField" : "$id", "$milField" : $mil, "$locField" : ${Json toJson geo} }""") head()

  private val locToJson: (Document) => String = (doc: Document) => (doc get locField).get.asDocument toJson()
  private val groupById: Seq[Document] => Map[String, Seq[Document]] =
    _ groupBy (d => (d get idField).get.asString.getValue)

  override def receiveLast(
                            ids: Seq[String],
                            geo: Option[(Double, Double, Double, Double)],
                            time: Long,
                            window: Long): Future[JsValue] = {
    val filters: Seq[Bson] = (if (ids != Nil) Filters.in(idField, ids: _*) :: Nil else Nil) ++
      (if (geo.isDefined) Filters.gte(locField + "." + GeoSnap.LATITUDE, geo.get._1) ::
        Filters.lte(locField + "." + GeoSnap.LATITUDE, geo.get._2) ::
        Filters.gte(locField + "." + GeoSnap.LONGITUDE, geo.get._3) ::
        Filters.lte(locField + "." + GeoSnap.LONGITUDE, geo.get._4) :: Nil else Nil) ++
      (Filters.gte(milField, time - (window * 1000)) :: Filters.lte(milField, time) :: Nil)
    geoSnaps find Filters.and(filters: _*) toFuture() map { ds =>
      JsObject(groupById(ds) map { case (id, seq) =>
        id -> Json.parse(
          s"""{ "location" : ${locToJson(seq.sortBy(_.get(milField).get.asNumber.longValue).reverse.head)} }""")
      }).as[JsValue]
    }
  }

  override def receiveRoute(
                             ids: Seq[String],
                             geo: Option[(Double, Double, Double, Double)],
                             time: Long,
                             window: Long): Future[JsValue] = {
    val (lat_l, lat_r, long_b, long_t) = if (geo.isDefined) geo.get else (0.0, 0.0, 0.0, 0.0)
    val inArea: ((String, Seq[Document])) => Boolean = {
      case (_, seq) =>
        (seq indexWhere { doc =>
          val loc = (doc get locField).get.asDocument
          val lat = loc.get(GeoSnap.LATITUDE).asDouble.getValue
          val long = loc.get(GeoSnap.LONGITUDE).asDouble.getValue
          (lat >= lat_l) && (lat <= lat_r) && (long >= long_b) && (long <= long_t)
        }) != -1
    }
    val filters: Seq[Bson] = (if (ids != Nil) Filters.in(idField, ids: _*) :: Nil else Nil) ++
      (Filters.gte(milField, time - (window * 1000)) :: Filters.lte(milField, time) :: Nil)
    geoSnaps find Filters.and(filters: _*) toFuture() map { ds =>
      JsObject((if (geo.isDefined) groupById(ds) filter inArea else groupById(ds)) map { case (id, seq) =>
        id -> JsObject(seq.sortBy(_.get(milField).get.asNumber.longValue).reverse map { doc =>
          val d = (doc get locField).get.asDocument
          (d get GeoSnap.TIME).asString.getValue -> (Json parse(Document(d) - GeoSnap.TIME toJson))
        }).as[JsValue]
      } toSeq)
    }
  }
}
