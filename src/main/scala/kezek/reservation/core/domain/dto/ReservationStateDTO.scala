package kezek.reservation.core.domain.dto

import io.circe.Json

trait ReservationStateDTO

object ReservationStateDTO {

  case class ApprovedDTO(name: String) extends ReservationStateDTO

  case class RejectedDTO(reason: String, name: String) extends ReservationStateDTO

  case class PaidDTO(paymentDetails: Json, name: String) extends ReservationStateDTO

  case class PreparingDTO(name: String) extends ReservationStateDTO

  case class CompletedDTO(name: String) extends ReservationStateDTO
}
