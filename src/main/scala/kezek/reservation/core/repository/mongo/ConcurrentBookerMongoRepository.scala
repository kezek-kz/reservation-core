package kezek.reservation.core.repository.mongo

import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.{ConcurrentBooker, Reservation}
import kezek.reservation.core.repository.ConcurrentBookerRepository
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConcurrentBookerMongoRepository()(implicit val mongoClient: MongoClient,
                                        implicit val executionContext: ExecutionContext)
  extends ConcurrentBookerRepository with MongoRepository with MainCodec {

  val config: Config = ConfigFactory.load()
  val database: MongoDatabase = mongoClient.getDatabase(config.getString("db.mongo.database"))
  val collection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.concurrent-booker"))

  override def increment(tableId: String): Future[ConcurrentBooker] = ???

  override def decrement(tableId: String): Future[ConcurrentBooker] = ???

  override def get(tableId: String): Future[ConcurrentBooker] =
    collection
      .find(equal("tableId", tableId))
      .first()
      .headOption()
      .map {
        case Some(document) => fromDocumentToReservation(document)
        case None => ConcurrentBooker(tableId, 0)
      }

  private def toDocument(concurrentBooker: ConcurrentBooker): Document = {
    Document(concurrentBooker.asJson.noSpaces)
  }

  private def fromDocumentToReservation(document: Document): ConcurrentBooker = {
    parse(document.toJson()).toTry match {
      case Success(json) =>
        json.as[ConcurrentBooker].toTry match {
          case Success(concurrentBooker) => concurrentBooker
          case Failure(exception) => throw exception
        }
      case Failure(exception) => throw exception
    }
  }


}
