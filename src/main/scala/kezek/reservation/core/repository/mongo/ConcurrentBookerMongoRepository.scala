package kezek.reservation.core.repository.mongo

import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.ConcurrentBooker
import kezek.reservation.core.repository.ConcurrentBookerRepository
import org.joda.time.DateTime
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.{FindOneAndDeleteOptions, FindOneAndUpdateOptions, UpdateOptions, Updates}
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ConcurrentBookerMongoRepository()(implicit val mongoClient: MongoClient,
                                        implicit val executionContext: ExecutionContext)
  extends ConcurrentBookerRepository with MongoRepository with MainCodec {

  val config: Config = ConfigFactory.load()
  val database: MongoDatabase = mongoClient.getDatabase(config.getString("db.mongo.database"))
  val collection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.concurrent-booker"))

  override def increment(tableId: String, date: DateTime, bookingTime: String): Future[_] =
    collection.findOneAndUpdate(
      and(
        equal("tableId", tableId),
        equal("date", date.getMillis),
        equal("bookingTime", bookingTime)
      ),
      Updates.inc("count", 1),
      FindOneAndUpdateOptions().upsert(true)
    ).toFuture()

  override def decrement(tableId: String, date: DateTime, bookingTime: String): Future[_] =
    collection.findOneAndUpdate(
      and(
        equal("tableId", tableId),
        equal("date", date.getMillis),
        equal("bookingTime", bookingTime)
      ),
      Updates.inc("count", -1),
      FindOneAndUpdateOptions().upsert(true)
    ).toFuture()


  override def get(tableId: String, date: DateTime, bookingTime: String): Future[ConcurrentBooker] =
    collection
      .find(
        and(
          equal("tableId", tableId),
          equal("date", date.getMillis),
          equal("bookingTime", bookingTime)
        )
      )
      .first()
      .headOption()
      .map {
        case Some(document) => fromDocumentToReservation(document)
        case None => ConcurrentBooker(tableId, date, bookingTime, 0)
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

  override def delete(tableId: String, date: DateTime, bookingTime: String): Future[_] =
    collection
      .deleteOne(
        and(
          equal("tableId", tableId),
          equal("date", date.getMillis),
          equal("bookingTime", bookingTime)
        )
      ).toFuture()
}
