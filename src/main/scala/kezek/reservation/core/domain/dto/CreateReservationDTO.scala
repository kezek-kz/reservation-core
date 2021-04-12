package kezek.reservation.core.domain.dto

import io.circe.Json
import org.joda.time.DateTime

case class CreateReservationDTO(slug: String,
                                customerId: String,
                                tables: Seq[String],
                                reservations: Seq[String],
                                date: DateTime,
                                paymentDetails: Option[Json])
