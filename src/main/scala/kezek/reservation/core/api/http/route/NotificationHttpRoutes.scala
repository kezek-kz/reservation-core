package kezek.reservation.core.api.http.route

import akka.NotUsed
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import io.circe.syntax.EncoderOps
import io.circe.generic.auto._
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.Reservation
import kezek.reservation.core.service.NotificationService

import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import javax.ws.rs.{GET, Path}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

trait NotificationHttpRoutes extends MainCodec {

  val notificationService: NotificationService
  implicit val executionContext: ExecutionContext

  def notificationHttpRoutes: Route = {
    pathPrefix("notifications") {
      concat(
        sseStream,
      )
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
  @Path("/notifications/{id}")
  @Tag(name = "Reservations")
  def sseStream: Route = {
    import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
    path(Segment) { tableId =>
      get {
        complete {
          Source
            .tick(2.seconds, 2.seconds, NotUsed)
            .flatMapConcat(_ => Source.future(notificationService.get(tableId)))
            .map(cb => ServerSentEvent(cb.asJson.noSpaces))
        }
      }
    }
  }


}
