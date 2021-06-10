package kezek.reservation.core.aws

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.amazonaws.HttpMethod
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{CannedAccessControlList, ObjectMetadata}
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}

import java.io.{File, InputStream}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class AwsS3Client(bucket: String)(implicit val s3Client: AmazonS3,
                                  implicit val executionContext: ExecutionContext,
                                  implicit val actorSystem: ActorSystem[_]) extends S3Client {

  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  override def uploadByteSource(inputStream: InputStream,
                                key: String,
                                fileInfo: FileInfo): Future[String] = Future {
    log.debug(s"uploadByteSource() was called {key: $key, contentType: ${fileInfo.contentType.toString()}}")
    val metadata: ObjectMetadata = new ObjectMetadata()
    metadata.setContentType(fileInfo.contentType.toString())
    s3Client.putObject(
      bucket,
      key,
      inputStream,
      metadata
    )
    s3Client.setObjectAcl(bucket, key, CannedAccessControlList.PublicRead)
    s3Client.getUrl(bucket, key).toExternalForm
  }



  override def delete(key: String): Future[Unit] = Future {
    log.debug(s"delete() was called {key: $key}")
    s3Client.deleteObject(bucket, key)
  }

  override def uploadFile(file: File, key: String): Future[String] = Future {
    log.debug(s"uploadFile() was called {key: $key}")
    s3Client.putObject(bucket, key, file)
    s3Client.setObjectAcl(bucket, key, CannedAccessControlList.PublicRead)
    s3Client.getUrl(bucket, key).toExternalForm
  }

}
