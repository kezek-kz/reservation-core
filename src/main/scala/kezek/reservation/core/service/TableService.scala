package kezek.reservation.core.service

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.amazonaws.services.s3.AmazonS3
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.scalaland.chimney.dsl.TransformerOps
import kezek.reservation.core.aws.{AwsS3Client, S3Client}
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.TableFilter.ByTableIdListFilter
import kezek.reservation.core.domain.dto.{CreateTableDTO, UpdateTableDTO}
import kezek.reservation.core.domain.{RestaurantMap, Table}
import kezek.reservation.core.exception.ApiException
import kezek.reservation.core.repository.TableRepository
import kezek.reservation.core.repository.mongo.TableMongoRepository
import org.mongodb.scala.MongoClient
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class TableService()(implicit val mongoClient: MongoClient,
                     implicit val executionContext: ExecutionContext,
                     implicit val system: ActorSystem[_],
                     implicit val s3Client: AmazonS3) extends MainCodec {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val tableRepository: TableRepository = new TableMongoRepository()
  val bucket: S3Client = new AwsS3Client("kezek")
  val bucketFolder: String = "table-qr"

  def update(id: String, updateTableDTO: UpdateTableDTO): Future[Table] = {
    log.debug(s"update() was called {id: $id, updateTableDTO: $updateTableDTO}")
    getById(id).flatMap { table =>
      tableRepository.update(
        id,
        updateTableDTO.into[Table]
          .withFieldConst(_.id, id)
          .withFieldConst(_.mapId, table.mapId)
          .withFieldConst(_.qrLink, Some(generateQr))
          .transform
      )
    }
  }

  def getById(id: String): Future[Table] = {
    log.debug(s"getById() was called {id: $id}")
    tableRepository.findById(id).map {
      case Some(table) => table
      case None =>
        log.error(s"getById() failed to find table {id: $id}")
        throw ApiException(StatusCodes.NotFound, s"Failed to find table with id: $id")
    }
  }

  def create(mapId: String, createTableDTO: CreateTableDTO): Future[Table] = {
    log.debug(s"create() was called {createTableDTO: ${createTableDTO.asJson.noSpaces}}")
    tableRepository.create(
      createTableDTO.into[Table]
        .withFieldConst(_.id, UUID.randomUUID().toString)
        .withFieldConst(_.mapId, mapId)
        .withFieldConst(_.qrLink, Some(generateQr()))
        .transform
    )
  }

  def delete(id: String): Future[Unit] = {
    log.debug(s"delete() was called {id: $id}")
    for(
      table <- getById(id);
      _ <- tableRepository.delete(id);
      _ <- deleteQR(table)
    ) yield ()
  }

  def generateQr(): String = "Some link to qr image"

  def countDeposit(tableIds: Seq[String]): Future[BigDecimal] = {
    log.debug(s"countDeposit() was called {tableIds: $tableIds}")
    tableRepository.findAll(Seq(ByTableIdListFilter(tableIds))).map {
      tables => tables.map(_.deposit).sum
    }
  }

  def saveQR(byteSource: Source[ByteString, Any],
             table: Table,
             fileInfo: FileInfo): Future[Table] = {
    log.debug(s"saveQR() was called {tableId: ${table.id}, fileName: ${fileInfo.fileName}, contentType: ${fileInfo.contentType.toString()}}")
    bucket.upload(byteSource, s"$bucketFolder/${table.id}", fileInfo).flatMap { qrLink =>
      tableRepository.update(
        table.id,
        table.copy(qrLink = Some(qrLink))
      )
    }
  }

  def deleteQR(table: Table): Future[Table] = {
    log.debug(s"saveQR() was called {tableId: ${table.id}, qrLink: ${table.qrLink}}")
    bucket.delete(s"$bucketFolder/${table.id}").flatMap { _ =>
      tableRepository.update(
        table.id,
        table.copy(qrLink = None)
      )
    }
  }

}

