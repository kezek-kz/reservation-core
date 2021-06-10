package kezek.reservation.core.repository

import kezek.reservation.core.domain.ConcurrentBooker

import scala.concurrent.Future

trait ConcurrentBookerRepository {

  def increment(tableId: String): Future[ConcurrentBooker]

  def decrement(tableId: String): Future[ConcurrentBooker]

  def get(tableId: String): Future[ConcurrentBooker]
}
