package kezek.reservation.core.repository

import akka.Done
import kezek.reservation.core.domain.ReservationFilter
import kezek.reservation.core.domain.{Reservation, ReservationFilter}
import kezek.reservation.core.util.SortType
import kezek.reservation.core.domain.ReservationFilter

import scala.concurrent.Future

trait ReservationRepository {

  def create(reservation: Reservation): Future[Reservation]

  def update(id: Long, reservation: Reservation): Future[Reservation]

  def findById(id: Long): Future[Option[Reservation]]

  def paginate(filters: Seq[ReservationFilter],
               page: Option[Int],
               pageSize: Option[Int],
               sortParams: Map[String, SortType]): Future[Seq[Reservation]]

  def count(filters: Seq[ReservationFilter]): Future[Long]

  def delete(id: Long): Future[Done]

  def incrementCounter(): Future[Unit]

  def getCounter(): Future[Long]
}
