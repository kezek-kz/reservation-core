package kezek.reservation.core

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.typesafe.config.{Config, ConfigFactory}
import kezek.reservation.core.api.http.HttpServer
import kezek.reservation.core.scripts.SeedScript
import kezek.reservation.core.service.{NotificationService, ReservationService, RestaurantMapService, TableService}
import org.mongodb.scala.MongoClient

import scala.concurrent.ExecutionContext

object Main extends App {

  implicit val config: Config = ConfigFactory.load()

  implicit val system: ActorSystem[Nothing] = ActorSystem[Nothing](
    Behaviors.empty,
    name = config.getString("akka.actor.system"),
    config
  )

  val awsCreds: BasicAWSCredentials = new BasicAWSCredentials(
    config.getString("aws.accessKeyId"),
    config.getString("aws.secretAccessKey")
  )

  implicit val s3Client: AmazonS3 = AmazonS3ClientBuilder.standard()
    .withRegion(config.getString("aws.region"))
    .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
    .build()


  implicit val mongoClient: MongoClient = MongoClient(config.getString("db.mongo.connection-string"))

  implicit val classicSystem: akka.actor.ActorSystem = system.classicSystem
  implicit val executionContext: ExecutionContext = classicSystem.dispatchers.lookup("akka.dispatchers.main")

  implicit val tableService: TableService = new TableService()
  implicit val restaurantMapService: RestaurantMapService = new RestaurantMapService()
  implicit val reservationService: ReservationService = new ReservationService()
  implicit val notificationService: NotificationService = new NotificationService()

  HttpServer().run()

  SeedScript.createReservationCollectionIndexes()

}
