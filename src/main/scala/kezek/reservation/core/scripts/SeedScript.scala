package kezek.reservation.core.scripts

import com.typesafe.config.{Config, ConfigFactory}
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes.{ascending, text}
import org.mongodb.scala.{Document, MongoClient, MongoCollection, MongoDatabase}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object SeedScript {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  def createReservationCollectionIndexes()(implicit mongoClient: MongoClient,
                                       executionContext: ExecutionContext): Unit = {
    log.debug(s"createReservationCollectionIndexes() was called")
    val database: MongoDatabase = mongoClient.getDatabase(config.getString("db.mongo.database"))
    val collection: MongoCollection[Document] = database.getCollection(config.getString("db.mongo.collection.reservation"))

    collection.createIndex(
      ascending("id"),
      IndexOptions().unique(true)
    ).toFuture().onComplete {
      case Success(_) =>
        log.debug("createReservationCollectionIndexes() successfully created unique indexes for id")
      case Failure(exception) =>
        log.error(s"createReservationCollectionIndexes() failed to create unique indexes for id{details: $exception}")
    }

  }


}
