package kezek.reservation.core.domain

import io.circe.Json
import org.joda.time.DateTime

trait ReservationState {
  def name: String
}

object ReservationState {

  final val CREATED = "СОЗДАН"
  final val APPROVED = "ПОДТВЕРЖДЕН"
  final val REJECTED = "ОТКАЗОНО"
  final val PAID = "ОПЛАЧЕН"
  final val PREPARING = "ГОТОВИТЬСЯ"
  final val COMPLETED = "ГОТОВО"

  case class Created(name: String, createdAt: DateTime) extends ReservationState {
    require(name == CREATED)
  }

  case class Approved(name: String, createdAt: DateTime) extends ReservationState {
    require(name == APPROVED)
  }

  case class Rejected(reason: String, name: String, createdAt: DateTime) extends ReservationState {
    require(name == REJECTED)
  }

  case class Paid(paymentDetails: Json, name: String, createdAt: DateTime) extends ReservationState {
    require(name == PAID)
  }

  case class Preparing(name: String, createdAt: DateTime) extends ReservationState {
    require(name == PREPARING)
  }

  case class Completed(name: String, createdAt: DateTime) extends ReservationState {
    require(name == COMPLETED)
  }

}
