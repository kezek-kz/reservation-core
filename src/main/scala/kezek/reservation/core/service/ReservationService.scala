package kezek.reservation.core.service

import akka.Done
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import com.typesafe.config.{Config, ConfigFactory}
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.scalaland.chimney.dsl.TransformerOps
import jdk.jshell.spi.ExecutionControl.NotImplementedException
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.ReservationFilter._
import kezek.reservation.core.domain.ReservationState._
import kezek.reservation.core.domain.dto.ReservationStateDTO._
import kezek.reservation.core.domain.dto.{CreateReservationDTO, ReservationListWithTotalDTO, ReservationStateDTO, UpdateReservationDTO}
import kezek.reservation.core.domain.{Reservation, ReservationFilter, ReservationState}
import kezek.reservation.core.exception.ApiException
import kezek.reservation.core.repository.ReservationRepository
import kezek.reservation.core.repository.mongo.MongoRepository.DUPLICATED_KEY_ERROR_CODE
import kezek.reservation.core.repository.mongo.ReservationMongoRepository
import kezek.reservation.core.util.SortType
import org.joda.time.DateTime
import org.mongodb.scala.{MongoClient, MongoWriteException}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

object ReservationService extends MainCodec {

  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  def generateFilters(categoryId: Option[String] = None,
                      title: Option[String] = None,
                      description: Option[String] = None): Seq[ReservationFilter] = {
    var filters: Seq[ReservationFilter] = Seq.empty
    if (categoryId.isDefined) filters = filters :+ ByCategoryIdFilter(categoryId.get)
    if (title.isDefined) filters = filters :+ ByTitleFilter(title.get)
    if (description.isDefined) filters = filters :+ ByDescriptionFilter(description.get)
    filters
  }

  def transformToReservationState(reservationStateDTO: ReservationStateDTO): ReservationState = {
    log.debug(s"transformToReservationState() was called {amlRequestStateDTO: ${reservationStateDTO.asJson.noSpaces}}")
    reservationStateDTO match {
      case WaitingPaymentDTO(name) => WaitingPayment(name, DateTime.now())
      case CanceledDTO(reason, name) => Canceled(reason, name, DateTime.now())
      case ReservedDTO(paymentDetails, name) => Reserved(paymentDetails, name, DateTime.now())
    }
  }

}

class ReservationService()(implicit val mongoClient: MongoClient,
                           implicit val executionContext: ExecutionContext,
                           implicit val tableService: TableService,
                           implicit val system: ActorSystem[_]) extends MainCodec {

  val config: Config = ConfigFactory.load()
  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)
  val reservationRepository: ReservationRepository = new ReservationMongoRepository()

  def paginate(filters: Seq[ReservationFilter],
               page: Option[Int],
               pageSize: Option[Int],
               sortParams: Map[String, SortType]): Future[ReservationListWithTotalDTO] = {
    log.debug(s"paginate() was called {filters: $filters, page: $page, pageSize: $pageSize, sortParams: $sortParams}")
    for (
      reservations <- reservationRepository.paginate(filters, page, pageSize, sortParams);
      count <- reservationRepository.count(filters)
    ) yield ReservationListWithTotalDTO(collection = reservations, total = count)
  }

  def setState(id: Long, newState: ReservationState): Future[Reservation] = {
    log.debug(s"setState() was called {id $id, newState: ${newState.asJson.noSpaces}}")
    getById(id).flatMap { reservation =>
      if (isStateChangeValid(reservation, newState)) {
        reservationRepository.update(id, reservation.changeState(newState))
      } else {
        log.error(s"setState() invalid state change {id $id, currentStatus: ${reservation.status}, newStatus: ${newState.name}}")
        throw ApiException(StatusCodes.Conflict, "Invalid state change")
      }
    }
  }

  def isStateChangeValid(reservation: Reservation, newState: ReservationState): Boolean = {
    log.debug(s"isStateChangeValid() was called {currentStatus: ${reservation.status}, newStatus: ${newState.name}}")
    (reservation.status, newState.name) match {
      case (CREATED, WAITING_PAYMENT) => true
      case (CREATED, CANCELED) => true
      case (WAITING_PAYMENT, CANCELED) => true
      case (WAITING_PAYMENT, RESERVED) => true
      case (RESERVED, CANCELED) => true
      case _ => false
    }
  }

  def update(id: Long, updateReservationDTO: UpdateReservationDTO): Future[Reservation] = {
    log.debug(s"update() was called {id: $id, updateReservationDTO: $updateReservationDTO}")
    for(
      deposit <- tableService.countDeposit(updateReservationDTO.tables);
      reservation <- getById(id);
      result <- reservationRepository.update(
        id,
        updateReservationDTO.into[Reservation]
          .withFieldConst(_.id, id)
          .withFieldConst(_.updatedAt, DateTime.now())
          .withFieldConst(_.createdAt, reservation.createdAt)
          .withFieldConst(_.rejectReason, reservation.rejectReason)
          .withFieldConst(_.status, reservation.status)
          .withFieldConst(_.states, reservation.states)
          .withFieldConst(_.deposit, deposit)
          .transform
      )
    ) yield result
  }

  def getById(id: Long): Future[Reservation] = {
    log.debug(s"getById() was called {id: $id}")
    reservationRepository.findById(id).map {
      case Some(reservation) => reservation
      case None =>
        log.error(s"getById() failed to find reservation {id: $id}")
        throw ApiException(StatusCodes.NotFound, s"Failed to find reservation with id: $id")
    }
  }

  def create(createReservationDTO: CreateReservationDTO): Future[Reservation] = {
    log.debug(s"create() was called {createReservationDTO: ${createReservationDTO.asJson.noSpaces}}")
    val dateTimeNow = DateTime.now()
    val initState = Created(CREATED, dateTimeNow)
    for (
      _ <- reservationRepository.incrementCounter();
      count <- reservationRepository.getCounter();
      deposit <- tableService.countDeposit(createReservationDTO.tables);
      reservation <- Future {
        createReservationDTO.into[Reservation]
          .withFieldConst(_.id, count)
          .withFieldConst(_.status, initState.name)
          .withFieldConst(_.createdAt, dateTimeNow)
          .withFieldConst(_.updatedAt, dateTimeNow)
          .withFieldConst(_.states, Seq(initState))
          .withFieldConst(_.rejectReason, None)
          .withFieldConst(_.paymentDetails, None)
          .withFieldConst(_.deposit, deposit)
          .transform
      };
      _ <- reservationRepository.create(reservation).recover {
        case ex: MongoWriteException if ex.getCode == DUPLICATED_KEY_ERROR_CODE =>
          log.error(s"create() failed to create reservation due to duplicate key {ex: $ex, reservation: ${reservation.asJson.noSpaces}")
          throw ApiException(StatusCodes.Conflict, s"Failed to create, reservation with id: ${reservation.id} already exists")
        case ex: Exception =>
          log.error(s"create() failed to create reservation {ex: $ex, reservation: ${reservation.asJson.noSpaces}}")
          throw ApiException(StatusCodes.ServiceUnavailable, ex.getMessage)
      }
    ) yield reservation
  }

  def delete(id: Long): Future[Done] = {
    log.debug(s"delete() was called {id: $id}")
    reservationRepository.delete(id)
  }

}

