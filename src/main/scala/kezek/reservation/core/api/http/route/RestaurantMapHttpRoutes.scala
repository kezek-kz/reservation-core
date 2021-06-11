package kezek.reservation.core.api.http.route

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
import kezek.reservation.core.codec.MainCodec
import kezek.reservation.core.domain.dto.{CreateTableDTO, UpdateTableDTO}
import kezek.reservation.core.domain.{RestaurantMap, Table}
import kezek.reservation.core.service.{RestaurantMapService, TableService}
import kezek.reservation.core.swagger.UploadMapMultipartRequest
import kezek.reservation.core.util.HttpUtil
import org.joda.time.DateTime

import javax.ws.rs.{GET, POST, PUT, Path}
import scala.util.{Failure, Success}

trait RestaurantMapHttpRoutes extends MainCodec {

  val restaurantMapService: RestaurantMapService
  val tableService: TableService

  def restaurantMapHttpRoutes: Route = {
    concat(
      pathPrefix("restaurant-maps") {
        concat(
          getRestaurantMapById,
          uploadRestaurantMap,
        )
      },
      pathPrefix("tables") {
        concat(
          getTableAllTables,
          getTableById,
          updateTable,
          addTable,
        )
      },
    )
  }
  @GET
  @Operation(
    summary = "Get restaurant map",
    description = "Returns restaurant map",
    method = "GET",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "restaurant-map", required = true),
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[RestaurantMap]),
            //            examples = Array(new ExampleObject(name = "RestaurantMap", value = "{\n  \"id\": \"restaurant-map\",\n  \"linkToSvg\": \"https://kezek.s3.eu-west-2.amazonaws.com/restaurant-maps/restaurant-map\"\n}"))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/restaurant-maps")
  @Tag(name = "Restaurant Map")
  def getRestaurantMapById: Route = {
    get {
      pathEndOrSingleSlash {
        onComplete(restaurantMapService.getById("restaurant-map")) {
          case Success(result) => complete(result)
          case Failure(exception) => HttpUtil.completeThrowable(exception)
        }
      }
    }
  }
  //
  //  @DELETE
  //  @Operation(
  //    summary = "Deletes restaurant map",
  //    description = "Deletes restaurant map",
  //    method = "DELETE",
  //    parameters = Array(
  //      new Parameter(name = "id", in = ParameterIn.PATH, example = "restaurant-map", required = true),
  //    ),
  //    responses = Array(
  //      new ApiResponse(
  //        responseCode = "204",
  //        description = "OK",
  //      ),
  //      new ApiResponse(responseCode = "500", description = "Internal server error")
  //    )
  //  )
  //  @Path("/restaurant-maps/{id}")
  //  @Tag(name = "Restaurant Map")
  //  def deleteRestaurantMap: Route = {
  //    delete {
  //      path(Segment) { id =>
  //        onComplete(restaurantMapService.delete(id)) {
  //          case Success(_) => complete(StatusCodes.NoContent)
  //          case Failure(exception) => HttpUtil.completeThrowable(exception)
  //        }
  //      }
  //    }
  //  }

  @POST
  @Operation(
    summary = "Upload restaurant map",
    description = "Uploads restaurant map to s3 and deletes old image",
    method = "POST",
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[UploadMapMultipartRequest]),
          mediaType = "multipart/form-data"
        )
      )
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[RestaurantMap]),
            examples = Array(new ExampleObject(name = "RestaurantMap", value = "{\n  \"id\": \"restaurant-map\",\n  \"linkToSvg\": \"https://kezek.s3.eu-west-2.amazonaws.com/restaurant-maps/restaurant-map\"\n}")),
            mediaType = "application/json"
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/restaurant-maps")
  @Tag(name = "Restaurant Map")
  def uploadRestaurantMap: Route = {
    post {
      pathEndOrSingleSlash {
        fileUpload("map") {
          case (fileInfo, byteSource) => {
            onComplete(restaurantMapService.upsert(byteSource, "restaurant-map", fileInfo)) {
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
    summary = "Get table by id",
    description = "Returns a full information about table by id",
    method = "GET",
    parameters = Array(
      new Parameter(name = "id", in = ParameterIn.PATH, example = "", required = true),
      new Parameter(name = "date", in = ParameterIn.QUERY, required = false),
      new Parameter(name = "bookingTime", in = ParameterIn.QUERY, required = false, schema = new Schema(allowableValues = Array("До обеда", "После обеда", "Вечер")))
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Table]),
            examples = Array(new ExampleObject(name = "Table", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/tables/{id}")
  @Tag(name = "Restaurant Map / Tables")
  def getTableById: Route = {
    get {
      path(Segment) { id =>
        parameters("date".as[DateTime].?, "bookingTime".?) { (date, bookingTime) =>
          onComplete(tableService.getById(id, date, bookingTime)) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }

  @GET
  @Operation(
    summary = "Get all tables",
    description = "Returns a list of tables",
    method = "GET",
    parameters = Array(
      new Parameter(name = "date", in = ParameterIn.QUERY, required = false),
      new Parameter(name = "bookingTime", in = ParameterIn.QUERY, required = false, schema = new Schema(allowableValues = Array("До обеда", "После обеда", "Вечер")))
    ),
    responses = Array(
      new ApiResponse(
        responseCode = "200",
        description = "OK",
        content = Array(
          new Content(
            schema = new Schema(implementation = classOf[Seq[Table]]),
            examples = Array(new ExampleObject(name = "Table", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/tables")
  @Tag(name = "Restaurant Map / Tables")
  def getTableAllTables: Route = {
    get {

      parameters("date".as[DateTime].?, "bookingTime".?) { (date, bookingTime) =>
        onComplete(tableService.getAllTables(date, bookingTime)) {
          case Success(result) => complete(result)
          case Failure(exception) => HttpUtil.completeThrowable(exception)
        }
      }
    }
  }

  @POST
  @Operation(
    summary = "Create table",
    description = "Creates new table",
    method = "POST",
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[CreateTableDTO]),
          mediaType = "application/json"
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
            schema = new Schema(implementation = classOf[Table]),
            examples = Array(new ExampleObject(name = "Table", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/tables")
  @Tag(name = "Restaurant Map / Tables")
  def addTable: Route = {
    post {
      pathEndOrSingleSlash {
        entity(as[CreateTableDTO]) { body =>
          onComplete(tableService.create(body)) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }

  @PUT
  @Operation(
    summary = "Update table",
    description = "Updates table",
    method = "PUT",
    requestBody = new RequestBody(
      content = Array(
        new Content(
          schema = new Schema(implementation = classOf[UpdateTableDTO]),
          mediaType = "application/json"
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
            schema = new Schema(implementation = classOf[Table]),
            examples = Array(new ExampleObject(name = "Table", value = ""))
          )
        )
      ),
      new ApiResponse(responseCode = "500", description = "Internal server error")
    )
  )
  @Path("/tables/{id}")
  @Parameter(name = "id", in = ParameterIn.PATH, required = true)
  @Tag(name = "Restaurant Map / Tables")
  def updateTable: Route = {
    put {
      path(Segment) { tableId =>
        entity(as[UpdateTableDTO]) { body =>
          onComplete(tableService.update(tableId, body)) {
            case Success(result) => complete(result)
            case Failure(exception) => HttpUtil.completeThrowable(exception)
          }
        }
      }
    }
  }


}
