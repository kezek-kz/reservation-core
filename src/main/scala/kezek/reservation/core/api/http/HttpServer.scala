package kezek.reservation.core.api.http

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.typesafe.config.{Config, ConfigFactory}
import kezek.reservation.core.swagger.SwaggerSite
import kezek.reservation.core.service.{NotificationService, ReservationService, RestaurantMapService, TableService}
import kezek.reservation.core.swagger.{SwaggerDocService, SwaggerSite}
import kezek.reservation.core.swagger.SwaggerSite
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

case class HttpServer()(implicit val actorSystem: ActorSystem[_],
                        implicit val executionContext: ExecutionContext,
                        implicit val tableService: TableService,
                        implicit val notificationService: NotificationService,
                        implicit val restaurantMapService: RestaurantMapService,
                        implicit val reservationService: ReservationService)
  extends HttpRoutes with SwaggerSite {

  implicit val config: Config = ConfigFactory.load()

  private val shutdown = CoordinatedShutdown(actorSystem)

  val log: Logger = LoggerFactory.getLogger(getClass.getSimpleName)

  def run(): Unit = {
    log.debug(s"start() PORT: '${config.getInt("http-server.port")}'")
    Http()
      .newServerAt(
        interface = config.getString("http-server.interface"),
        port = config.getInt("http-server.port")
      )
      .bind(
        cors(CorsSettings(config)) { concat (routes, swaggerSiteRoute, new SwaggerDocService().routes) }
      )
      .onComplete {
        case Success(binding) =>
          val address = binding.localAddress
          actorSystem.log.info("reservation-core online at http://{}:{}/", address.getHostString, address.getPort)

          shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-graceful-terminate") { () =>
            binding.terminate(10.seconds).map { _ =>
              actorSystem.log
                .info("reservation-core http://{}:{}/ graceful shutdown completed", address.getHostString, address.getPort)
              Done
            }
          }
        case Failure(ex) =>
          actorSystem.log.error("Failed to bind HTTP endpoint, terminating system", ex)
          actorSystem.terminate()
      }
  }

}
