package kezek.reservation.core.api.http.route

import akka.NotUsed
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import akka.http.scaladsl.server.PathMatchers.Segment
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.{Content, ExampleObject, Schema}
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.Reservation
import kezek.reservation.core.service.NotificationService
import org.joda.time.DateTime

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
      new Parameter(name = "tableId", in = ParameterIn.PATH, example = "", required = true),
      new Parameter(name = "date", in = ParameterIn.QUERY, required = true),
      new Parameter(name = "bookingTime", in = ParameterIn.QUERY, required = true, schema = new Schema(allowableValues = Array("До обеда", "После обеда", "Вечер"))),
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
  @Path("/notifications/table/{tableId}")
  @Tag(name = "Reservations")
  def sseStream: Route = {
    get {
      path("table" / Segment) { tableId =>
        parameters(
          "date".as[DateTime],
          "bookingTime"
        ) {
          (date, bookingTime) => {
            import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
            complete {
              Source
                .tick(2.seconds, 2.seconds, NotUsed)
                .flatMapConcat(_ => Source.future(notificationService.get(tableId, date, bookingTime)))
                .map(cb => ServerSentEvent(cb.asJson.noSpaces))
            }
          }
        }
      }
    }
  }


}
