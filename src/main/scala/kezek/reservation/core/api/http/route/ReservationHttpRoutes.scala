package kezek.reservation.core.api.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import kezek.reservation.core.domain.dto.ReservationListWithTotalDTO
import kezek.reservation.core.util.SortUtil
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.Reservation
import kezek.reservation.core.domain.dto.{CreateReservationDTO, ReservationListWithTotalDTO, ReservationStateDTO, UpdateReservationDTO}
import kezek.reservation.core.service.ReservationService
import kezek.reservation.core.util.{HttpUtil, SortUtil}
import kezek.reservation.core.domain.dto.ReservationListWithTotalDTO
import kezek.reservation.core.util.SortUtil

import javax.ws.rs._
import scala.util.{Failure, Success}

trait ReservationHttpRoutes extends MainCodec {

  val reservationService: ReservationService

  def reservationHttpRoutes: Route = {
    pathPrefix("reservations") {
      concat(
        updateReservationStatus,
        updateReservation,
        getReservationById,
        deleteReservation,
        paginateReservations,
        createReservation
      )
    }
  }

  @PUT
  @Operation(
    summary = "Update reservation status",
    description = "Updates reservation's status and appends state to states",
    method = "PUT",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
    ),
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[ReservationStateDTO]),
          mediaType = "application/json",
          examples = Array(
            new ExampleObject(name = "CancelDTO", value = "{\n  \"name\": \"ОТМЕНЕН\",\n  \"reason\": \"не успел\"\n}"),
            new ExampleObject(name = "ReservedDTO", value = "{\n  \"name\": \"ЗАБРОНИРОВАНО\",\n  \"paymentDetails\": {}\n}"),
            new ExampleObject(name = "WaitingPaymentDTO", value = "{\n  \"name\": \"ОЖИДАНИЕ ОПЛАТЫ\"}"),
          )
        ),
      ),
      required = true
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Reservation]),
            examples = Array(new ExampleObject(name = "Reservation", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/reservations/{id}/status")
  @Tag(name = "Reservations")
  def updateReservationStatus: Route = {
    put {
      path(LongNumber / "status") { id =>
        entity(as[ReservationStateDTO]) { body =>
          onComplete(reservationService.setState(id, ReservationService.transformToReservationState(body))) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }

  @GET
  @Operation(
    summary = "Get reservation list",
    description = "Get filtered and paginated reservation list",
    method = "GET",
    parameters = Array(
      new Parameter(name = "page", in = ParameterIn.QUERY, example = "1"),
      new Parameter(name = "pageSize", in = ParameterIn.QUERY, example = "10"),
      new Parameter(name = "sort", in = ParameterIn.QUERY, example = "")
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[ReservationListWithTotalDTO]),
            mediaType = "application/json",
            examples = Array(new ExampleObject(name = "ReservationListWithTotalDTO", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/reservations")
  @Tag(name = "Reservations")
  def paginateReservations: Route = {
    get {
      pathEndOrSingleSlash {
        parameters(
          "page".as[Int].?,
          "pageSize".as[Int].?,
          "sort".?
        ) {
          (page,
           pageSize,
           sort) => {
            onComplete {
              reservationService.paginate(
                ReservationService.generateFilters(
                ),
                page,
                pageSize,
                SortUtil.parseSortParams(sort)
              )
            } {
              case Success(result) => complete(result)
              case Failure(exception) => HttpUtil.completeThrowable(exception)
            }
          }
        }
      }
    }
  }

  @GET
  @Operation(
    summary = "Get reservation by id",
    description = "Returns a full information about reservation by id",
    method = "GET",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Reservation]),
            examples = Array(new ExampleObject(name = "Reservation", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/reservations/{id}")
  @Tag(name = "Reservations")
  def getReservationById: Route = {
    get {
      path(LongNumber) { id =>
        onComplete(reservationService.getById(id)) {
          case Success(result) => complete(result)
          case Failure(exception) => HttpUtil.completeThrowable(exception)
        }
      }
    }
  }

  @POST
  @Operation(
    summary = "Create reservation",
    description = "Creates new reservation",
    method = "POST",
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[CreateReservationDTO]),
          mediaType = "application/json",
          examples = Array(
            //            new ExampleObject(name = "CreateReservationDTO", value = "")
          )
        )
      ),
      required = true
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Reservation]),
            examples = Array(new ExampleObject(name = "Reservation", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/reservations")
  @Tag(name = "Reservations")
  def createReservation: Route = {
    post {
      pathEndOrSingleSlash {
        entity(as[CreateReservationDTO]) { body =>
          onComplete(reservationService.create(body)) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }

  @PUT
  @Operation(
    summary = "Update reservation",
    description = "Updates reservation",
    method = "PUT",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
    ),
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[UpdateReservationDTO]),
          mediaType = "application/json",
          examples = Array(new ExampleObject(name = "UpdateReservationDTO", value = ""))
        )
      ),
      required = true
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Reservation]),
            examples = Array(new ExampleObject(name = "Reservation", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/reservations/{id}")
  @Tag(name = "Reservations")
  def updateReservation: Route = {
    put {
      path(LongNumber) { id =>
        entity(as[UpdateReservationDTO]) { body =>
          onComplete(reservationService.update(id, body)) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }

  @DELETE
  @Operation(
    summary = "Deletes reservation",
    description = "Deletes reservation",
    method = "DELETE",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "204",
        description = "OK",
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/reservations/{id}")
  @Tag(name = "Reservations")
  def deleteReservation: Route = {
    delete {
      path(LongNumber) { id =>
        onComplete(reservationService.delete(id)) {
          case Success(_) => complete(StatusCodes.NoContent)
          case Failure(exception) => HttpUtil.completeThrowable(exception)
        }
      }
    }
  }

}
