package kezek.reservation.core.api.http

import akka.NotUsed
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Source
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kezek.reservation.core.api.http.route.{NotificationHttpRoutes, ReservationHttpRoutes, RestaurantMapHttpRoutes}

import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import javax.ws.rs.{GET, Path}
import scala.concurrent.duration.DurationInt

@Path("/api")
trait HttpRoutes
  extends ReservationHttpRoutes
  with RestaurantMapHttpRoutes
  with NotificationHttpRoutes {

  val routes: Route = {
    pathPrefix("api") {
      concat(
        healthcheck,
        reservationHttpRoutes,
        restaurantMapHttpRoutes,
        notificationHttpRoutes
      )
    }
  }

  @GET
  @Operation(
    summary = "health check",
    method = "GET",
    responses = Array(
      new ApiResponse(responseCode = "200", description = "OK"),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/healthcheck")
  @Tag(name = "Healthcheck")
  def healthcheck: Route = {
    path("healthcheck") { ctx =>
      complete("ok")(ctx)
    }
  }
}
