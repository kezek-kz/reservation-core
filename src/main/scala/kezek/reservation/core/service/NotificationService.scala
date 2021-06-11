package kezek.reservation.core.service

import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.ConcurrentBooker
import kezek.reservation.core.repository.ConcurrentBookerRepository
import kezek.reservation.core.repository.mongo.ConcurrentBookerMongoRepository
import org.joda.time.DateTime
import org.mongodb.scala.MongoClient
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

class NotificationService()(implicit val mongoClient: MongoClient,
                            implicit val executionContext: ExecutionContext,
                            implicit val system: ActorSystem[_]) extends MainCodec {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val concurrentBookerRepository: ConcurrentBookerRepository = new ConcurrentBookerMongoRepository()


  def increment(tableId: String, date: DateTime, bookingTime: String): Future[_] = {
    concurrentBookerRepository.increment(tableId, date, bookingTime)
  }

  def decrement(tableId: String, date: DateTime, bookingTime: String): Future[_] = {
    concurrentBookerRepository.decrement(tableId, date, bookingTime)
  }

  def reset(tableId: String, date: DateTime, bookingTime: String): Future[_] = {
    concurrentBookerRepository.delete(tableId: String, date: DateTime, bookingTime: String)
  }

  def get(tableId: String, date: DateTime, bookingTime: String): Future[ConcurrentBooker] = {
    concurrentBookerRepository.get(tableId, date.withTimeAtStartOfDay(), bookingTime)
  }

}
