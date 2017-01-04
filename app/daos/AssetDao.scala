package daos

import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.mongodb.client.result.{DeleteResult, UpdateResult}
import org.mongodb.scala.model.Filters
import org.mongodb.scala.{Completed, Document, MongoCollection}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import services.Mongo

import scala.concurrent.Future

@ImplementedBy(classOf[MongoAssetDao])
trait AssetDao {
  def exists(id: String): Future[Boolean]

  def create(id: String, meta: JsValue): Future[Completed]

  def receive(id: String): Future[JsValue]

  def update(id: String, meta: JsValue): Future[UpdateResult]

  def delete(id: String): Future[DeleteResult]
}

@Singleton
class MongoAssetDao @Inject()(mongo: Mongo) extends AssetDao {
  private val assets: MongoCollection[Document] = mongo.db getCollection "assets"
  private val idMatch = Filters.eq("_id", _: String)

  override def exists(id: String): Future[Boolean] = assets count idMatch(id) head() map (_ > 0)

  override def create(id: String, meta: JsValue): Future[Completed] =
    assets insertOne Document(meta toString) + ("_id" -> id) head()

  override def receive(id: String): Future[JsValue] = assets find idMatch(id) head() map
    (d => Json parse (d - "_id" toJson))

  override def update(id: String, meta: JsValue): Future[UpdateResult] = assets find idMatch(id) head() flatMap {
    doc => {
      val j = JsObject(((Json parse doc.toJson).as[JsObject] deepMerge meta.as[JsObject]).fields filter {
        case (_, JsNull) => false
        case (_, JsString("")) => false
        case _ => true
      })
      assets replaceOne(idMatch(id), Document(j toString)) head()
    }
  }

  override def delete(id: String): Future[DeleteResult] = assets deleteOne idMatch(id) head()
}
