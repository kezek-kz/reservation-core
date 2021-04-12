package kezek.reservation.core.repository.mongo

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import com.mongodb.client.model.ReplaceOptions
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.RestaurantMap
import kezek.reservation.core.exception.ApiException
import kezek.reservation.core.repository.RestaurantMapRepository
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RestaurantMapMongoRepository()(implicit val mongoClient: MongoClient,
                                     implicit val executionContext: ExecutionContext)
  extends RestaurantMapRepository with MainCodec with MongoRepository {

  override val sortingFields: Seq[String] = Seq("phoneNumber", "firstName")
  val config: Config = ConfigFactory.load()
  val database: MongoDatabase = mongoClient.getDatabase(config.getString("db.mongo.database"))
  val collection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.restaurant-map"))

  override def upsert(id: String, restaurantMap: RestaurantMap): Future[RestaurantMap] = {
    collection
      .replaceOne(
        equal("id", id),
        toDocument(restaurantMap),
        new ReplaceOptions().upsert(true)
      )
      .head()
      .map(_ => restaurantMap)
  }

  private def toDocument(restaurantMap: RestaurantMap): Document = {
    Document(restaurantMap.asJson.noSpaces)
  }

  override def findById(id: String): Future[Option[RestaurantMap]] = {
    collection
      .find(equal("id", id))
      .first()
      .headOption()
      .map {
        case Some(document) => Some(fromDocumentToRestaurantMap(document))
        case None => None
      }
  }

  private def fromDocumentToRestaurantMap(document: Document): RestaurantMap = {
    parse(document.toJson()).toTry match {
      case Success(json) =>
        json.as[RestaurantMap].toTry match {
          case Success(restaurantMap) => restaurantMap
          case Failure(exception) => throw exception
        }
      case Failure(exception) => throw exception
    }
  }

  override def delete(id: String): Future[Unit] = {
    collection.deleteOne(equal("id", id)).head().map { deleteResult =>
      if (deleteResult.wasAcknowledged() && deleteResult.getDeletedCount == 1) {
        Done
      } else {
        throw ApiException(StatusCodes.NotFound, "Failed to delete restaurantMap")
      }
    }
  }
}