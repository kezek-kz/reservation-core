package kezek.reservation.core.domain

import org.joda.time.DateTime

case class ConcurrentBooker(tableId: String, date: DateTime, bookingTime: String, count: Long)
