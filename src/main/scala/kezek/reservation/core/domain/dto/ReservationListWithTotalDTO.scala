package kezek.reservation.core.domain.dto

import kezek.reservation.core.domain.Reservation

case class ReservationListWithTotalDTO(total: Long, collection: Seq[Reservation])
