package kezek.reservation.core.domain

import io.circe.Json
import org.joda.time.DateTime

trait ReservationState {
  def name: String
}

object ReservationState {

  final val CREATED = "СОЗДАН"
  final val WAITING_PAYMENT = "ОЖИДАНИЕ ОПЛАТЫ"
  final val CANCELED = "ОТМЕНЕН"
  final val RESERVED = "ЗАБРОНИРОВАНО"

  case class Created(name: String, createdAt: DateTime) extends ReservationState {
    require(name == CREATED)
  }

  case class WaitingPayment(name: String, createdAt: DateTime) extends ReservationState {
    require(name == WAITING_PAYMENT)
  }

  case class Canceled(reason: String, name: String, createdAt: DateTime) extends ReservationState {
    require(name == CANCELED)
  }

  case class Reserved(paymentDetails: Json, name: String, createdAt: DateTime) extends ReservationState {
    require(name == RESERVED)
  }
}
