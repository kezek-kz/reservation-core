package kezek.reservation.core.domain

import io.circe.Json
import ReservationState.{Paid, Rejected}
import org.joda.time.DateTime

case class Reservation(id: Long,
                       slug: String,
                       customerId: String,
                       tables: Seq[String],
                       reservations: Seq[String],
                       date: DateTime,
                       createdAt: DateTime,
                       updatedAt: DateTime,
                       deposit: BigDecimal,
                       rejectReason: Option[String],
                       paymentDetails: Option[Json],
                       status: String,
                       states: Seq[ReservationState]) {

  def changeState(newState: ReservationState): Reservation = {
    (newState match {
      case s: Rejected => this.copy(rejectReason = Some(s.reason))
      case s: Paid => this.copy(paymentDetails = Some(s.paymentDetails))
      case _ => this
    }).copy(
      status = newState.name,
      updatedAt = DateTime.now(),
      states = states :+ newState
    )
  }

}