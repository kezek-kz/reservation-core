package kezek.reservation.core.service

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentType, ContentTypes, StatusCodes}
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.{Config, ConfigFactory}
import kezek.reservation.core.aws.{AwsS3Client, S3Client}
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.RestaurantMap
import kezek.reservation.core.exception.ApiException
import kezek.reservation.core.repository.RestaurantMapRepository
import kezek.reservation.core.repository.mongo.RestaurantMapMongoRepository
import org.mongodb.scala.MongoClient
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class RestaurantMapService()(implicit val mongoClient: MongoClient,
                             implicit val executionContext: ExecutionContext,
                             implicit val system: ActorSystem[_],
                             implicit val s3Client: AmazonS3) extends MainCodec {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val restaurantMapRepository: RestaurantMapRepository = new RestaurantMapMongoRepository()
  val restaurantMapBucket: S3Client = new AwsS3Client("kezek")
  val bucketFolder: String = "restaurant-maps"


  def getById(id: String): Future[RestaurantMap] = {
    log.debug(s"getById() was called {id: $id}")
    restaurantMapRepository.findById(id).map {
      case Some(restaurantMap) => restaurantMap
      case None =>
        log.error(s"getById() failed to find restaurantMap {id: $id}")
        throw ApiException(StatusCodes.NotFound, s"Failed to find restaurantMap with id: $id")
    }
  }

  def upsert(byteSource: Source[ByteString, Any],
             restaurantMapId: String,
             fileInfo: FileInfo): Future[RestaurantMap] = {
    log.debug(s"uploadRestaurantMapImage() was called {fileName: ${fileInfo.fileName}, contentType: ${fileInfo.contentType.toString()}}")
    if(!isSvgFile(fileInfo.contentType)) {
      throw ApiException(StatusCodes.BadRequest, s"File content type 'image/svg+xml' required, but ${fileInfo.contentType.toString()} provided")
    }
    for(
      _ <- restaurantMapBucket.delete(s"$bucketFolder/$restaurantMapId");
      svgUrl <- restaurantMapBucket.upload(byteSource, s"$bucketFolder/$restaurantMapId", fileInfo);
      restaurantMap <- restaurantMapRepository.upsert(
        restaurantMapId,
        RestaurantMap(restaurantMapId, svgUrl)
      )
    ) yield restaurantMap
  }

  def isSvgFile(contentType: ContentType): Boolean = {
    contentType.toString() == "image/svg+xml"
  }

  def delete(id: String): Future[Unit] = {
    log.debug(s"delete() was called {id: $id}")
    for(
      _ <- restaurantMapBucket.delete(s"$bucketFolder/$id");
      _ <- restaurantMapRepository.delete(id)
    ) yield ()
  }

}

