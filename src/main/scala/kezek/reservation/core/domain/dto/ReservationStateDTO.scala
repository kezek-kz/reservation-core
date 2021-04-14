package kezek.reservation.core.domain.dto

import io.circe.Json

trait ReservationStateDTO

object ReservationStateDTO {

  case class CanceledDTO(reason: String, name: String) extends ReservationStateDTO

  case class ReservedDTO(paymentDetails: Json, name: String) extends ReservationStateDTO

  case class WaitingPaymentDTO(name: String) extends ReservationStateDTO

}
