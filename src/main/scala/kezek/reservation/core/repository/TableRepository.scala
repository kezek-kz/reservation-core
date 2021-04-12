package kezek.reservation.core.repository

import kezek.reservation.core.domain.{Table, TableFilter}

import scala.concurrent.Future

trait TableRepository {

  def create(table: Table): Future[Table]

  def update(id: String, table: Table): Future[Table]

  def findById(id: String): Future[Option[Table]]

  def findAll(filters: Seq[TableFilter]): Future[Seq[Table]]

  def count(filters: Seq[TableFilter]): Future[Long]

  def delete(id: String): Future[Unit]

}
