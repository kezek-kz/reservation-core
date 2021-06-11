package kezek.reservation.core.repository

import kezek.reservation.core.domain.ConcurrentBooker
import org.joda.time.DateTime

import scala.concurrent.Future

trait ConcurrentBookerRepository {
  def delete(tableId: String, date: DateTime, bookingTime: String): Future[_]

  def increment(tableId: String, date: DateTime, bookingTime: String): Future[_]

  def decrement(tableId: String, date: DateTime, bookingTime: String): Future[_]

  def get(tableId: String, date: DateTime, bookingTime: String): Future[ConcurrentBooker]
}
