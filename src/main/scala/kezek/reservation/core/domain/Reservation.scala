package kezek.reservation.core.domain

import io.circe.Json
import ReservationState.{Reserved, Canceled}
import org.joda.time.DateTime

case class Reservation(id: Long,
                       customerId: String,
                       tables: Seq[String],
                       date: DateTime,
                       bookingTime: String,
                       createdAt: DateTime,
                       updatedAt: DateTime,
                       deposit: BigDecimal,
                       rejectReason: Option[String],
                       paymentDetails: Option[Json],
                       status: String,
                       states: Seq[ReservationState]) {

  def changeState(newState: ReservationState): Reservation = {
    (newState match {
      case s: Canceled => this.copy(rejectReason = Some(s.reason))
      case s: Reserved => this.copy(paymentDetails = Some(s.paymentDetails))
      case _ => this
    }).copy(
      status = newState.name,
      updatedAt = DateTime.now(),
      states = states :+ newState
    )
  }

}