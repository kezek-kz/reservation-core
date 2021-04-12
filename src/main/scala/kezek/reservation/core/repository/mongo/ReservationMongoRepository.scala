package kezek.reservation.core.repository.mongo

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import kezek.reservation.core.domain.ReservationFilter._
import kezek.reservation.core.domain.ReservationFilter
import ReservationMongoRepository.{Counter, fromFiltersToBson}
import kezek.reservation.core.util.SortType
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.{Reservation, ReservationFilter}
import kezek.reservation.core.exception.ApiException
import kezek.reservation.core.repository.ReservationRepository
import kezek.reservation.core.util.{PaginationUtil, SortType}
import kezek.reservation.core.domain.ReservationFilter
import kezek.reservation.core.util.SortType
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.UpdateOptions
import org.mongodb.scala.model.Updates.{combine, inc, setOnInsert}
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ReservationMongoRepository {

  private def fromFiltersToBson(filters: Seq[ReservationFilter]): Bson = {
    if (filters.isEmpty) Document()
    else and(
      filters.map {
        case ByCategoryIdFilter(categoryId) => equal("categories", categoryId)
        case other =>
          throw new RuntimeException(s"Failed to generate bson filter: $other not implemented")
      }: _*
    )
  }

  case class Counter(id: String, count: Long)

}

class ReservationMongoRepository()(implicit val mongoClient: MongoClient,
                             implicit val executionContext: ExecutionContext)
  extends ReservationRepository with MainCodec with MongoRepository {

  override val sortingFields: Seq[String] = Seq("phoneNumber", "firstName")
  val config: Config = ConfigFactory.load()
  val database: MongoDatabase = mongoClient.getDatabase(config.getString("db.mongo.database"))
  val collection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.reservation"))
  val counterCollection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.counter"))

  override def  incrementCounter(): Future[Unit] = {
    counterCollection.updateOne(
      equal("id", "reservation"),
      combine(
        setOnInsert("id", "reservation"),
        inc("count", 1),
      ),
      UpdateOptions().upsert(true)
    ).head().map(_ => ())
  }

  override def getCounter(): Future[Long] = {
    counterCollection
      .find(equal("id", "reservation"))
      .headOption()
      .map {
        case Some(document) => parse(document.toJson()).toTry.get.hcursor.get[Long]("count").toTry.get
        case None => 0L
      }
  }

  override def create(reservation: Reservation): Future[Reservation] = {
    collection.insertOne(toDocument(reservation)).head().map(_ => reservation)
  }

  private def toDocument(reservation: Reservation): Document = {
    Document(reservation.asJson.noSpaces)
  }

  override def update(id: Long, reservation: Reservation): Future[Reservation] = {
    collection.replaceOne(equal("id", id), toDocument(reservation)).head().map { updateResult =>
      if (updateResult.wasAcknowledged()) {
        reservation
      } else {
        throw new RuntimeException(s"Failed to replace reservation with id: $id")
      }
    }
  }

  override def findById(id: Long): Future[Option[Reservation]] = {
    collection
      .find(equal("id", id))
      .first()
      .headOption()
      .map {
        case Some(document) => Some(fromDocumentToReservation(document))
        case None => None
      }
  }

  private def fromDocumentToReservation(document: Document): Reservation = {
    parse(document.toJson()).toTry match {
      case Success(json) =>
        json.as[Reservation].toTry match {
          case Success(reservation) => reservation
          case Failure(exception) => throw exception
        }
      case Failure(exception) => throw exception
    }
  }

  override def paginate(filters: Seq[ReservationFilter],
                        page: Option[Int],
                        pageSize: Option[Int],
                        sortParams: Map[String, SortType]): Future[Seq[Reservation]] = {
    val filtersBson = fromFiltersToBson(filters)
    val sortBson = fromSortParamsToBson(sortParams)
    val limit = pageSize.getOrElse(10)
    val offset = PaginationUtil.offset(page = page.getOrElse(1), size = limit)

    collection
      .find(filtersBson)
      .sort(sortBson)
      .skip(offset)
      .limit(limit)
      .toFuture()
      .map(documents => documents map fromDocumentToReservation)
  }

  override def count(filters: Seq[ReservationFilter]): Future[Long] = {
    collection.countDocuments(fromFiltersToBson(filters)).head()
  }

  override def delete(id: Long): Future[Done] = {
    collection.deleteOne(equal("id", id)).head().map { deleteResult =>
      if (deleteResult.wasAcknowledged() && deleteResult.getDeletedCount == 1) {
        Done
      } else {
        throw ApiException(StatusCodes.NotFound, "Failed to delete reservation")
      }
    }
  }
}
