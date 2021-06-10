package kezek.reservation.core.domain.dto

import io.circe.Json
import org.joda.time.DateTime

case class UpdateReservationDTO(                                customerId: String,
                                tables: Seq[String],
                                reservations: Seq[String],
                                bookingTime: String,
                                date: DateTime,
                                deposit: BigDecimal,
                                paymentDetails: Option[Json])
