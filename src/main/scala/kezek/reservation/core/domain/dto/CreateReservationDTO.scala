package kezek.reservation.core.domain.dto

import io.circe.Json
import org.joda.time.DateTime

case class CreateReservationDTO(customerId: String,
                                tables: Seq[String],
                                bookingTime: String,
                                date: DateTime,
                                paymentDetails: Option[Json])
