package kezek.reservation.core.service

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.ConcurrentBooker
import kezek.reservation.core.repository.ConcurrentBookerRepository
import kezek.reservation.core.repository.mongo.ConcurrentBookerMongoRepository
import org.mongodb.scala.MongoClient
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class NotificationService()(implicit val mongoClient: MongoClient,
                            implicit val executionContext: ExecutionContext,
                            implicit val system: ActorSystem[_]) extends MainCodec {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val concurrentBookerRepository: ConcurrentBookerRepository = new ConcurrentBookerMongoRepository()


  def increment(tableId: String): Future[ConcurrentBooker] = {
    concurrentBookerRepository.increment(tableId)
  }

  def decrement(tableId: String): Future[ConcurrentBooker] = {
    concurrentBookerRepository.decrement(tableId)
  }

  def get(tableId: String): Future[ConcurrentBooker] = {
    concurrentBookerRepository.get(tableId)
  }

}
