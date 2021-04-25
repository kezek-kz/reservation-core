package kezek.reservation.core.api.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import kezek.reservation.core.api.http.route.{ReservationHttpRoutes, RestaurantMapHttpRoutes}

import javax.ws.rs.{GET, Path}

@Path("/api")
trait HttpRoutes
  extends ReservationHttpRoutes
  with RestaurantMapHttpRoutes {

  val routes: Route = {
    pathPrefix("api") {
      concat(
        healthcheck,
        reservationHttpRoutes,
        restaurantMapHttpRoutes
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
