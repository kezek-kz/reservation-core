package kezek.reservation.core.repository.mongo

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.TableFilter.{ByStateFilter, ByTableIdListFilter}
import kezek.reservation.core.domain.{Table, TableFilter}
import kezek.reservation.core.exception.ApiException
import kezek.reservation.core.repository.TableRepository
import kezek.reservation.core.repository.mongo.TableMongoRepository.fromFiltersToBson
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object TableMongoRepository {

  private def fromFiltersToBson(filters: Seq[TableFilter]): Bson = {
    if (filters.isEmpty) Document()
    else and(
      filters.map {
        case ByTableIdListFilter(tableIds) => in("id", tableIds: _*)
        case ByStateFilter(state) => equal("state", state)
        case other =>
          throw new RuntimeException(s"Failed to generate bson filter: $other not implemented")
      }: _*
    )
  }

}

class TableMongoRepository()(implicit val mongoClient: MongoClient,
                             implicit val executionContext: ExecutionContext)
  extends TableRepository with MainCodec with MongoRepository {

  override val sortingFields: Seq[String] = Seq("phoneNumber", "firstName")
  val config: Config = ConfigFactory.load()
  val database: MongoDatabase = mongoClient.getDatabase(config.getString("db.mongo.database"))
  val collection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.table"))

  override def create(table: Table): Future[Table] = {
    collection.insertOne(toDocument(table)).head().map(_ => table)
  }

  private def toDocument(table: Table): Document = {
    Document(table.asJson.noSpaces)
  }

  override def update(id: String, table: Table): Future[Table] = {
    collection.replaceOne(equal("id", id), toDocument(table)).head().map { updateResult =>
      if (updateResult.wasAcknowledged()) {
        table
      } else {
        throw new RuntimeException(s"Failed to replace table with id: $id")
      }
    }
  }

  override def findById(id: String): Future[Option[Table]] = {
    collection
      .find(equal("id", id))
      .first()
      .headOption()
      .map {
        case Some(document) => Some(fromDocumentToTable(document))
        case None => None
      }
  }

  private def fromDocumentToTable(document: Document): Table = {
    parse(document.toJson()).toTry match {
      case Success(json) =>
        json.as[Table].toTry match {
          case Success(table) => table
          case Failure(exception) => throw exception
        }
      case Failure(exception) => throw exception
    }
  }

  override def findAll(filters: Seq[TableFilter]): Future[Seq[Table]] = {
    val filtersBson = fromFiltersToBson(filters)

    collection
      .find(filtersBson)
      .toFuture()
      .map(documents => documents map fromDocumentToTable)
  }

  override def count(filters: Seq[TableFilter]): Future[Long] = {
    collection.countDocuments(fromFiltersToBson(filters)).head()
  }

  override def delete(id: String): Future[Unit] = {
    collection.deleteOne(equal("id", id)).head().map { deleteResult =>
      if (deleteResult.wasAcknowledged() && deleteResult.getDeletedCount == 1) {
        Done
      } else {
        throw ApiException(StatusCodes.NotFound, "Failed to delete table")
      }
    }
  }
}
