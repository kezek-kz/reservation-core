package kezek.reservation.core.repository

import kezek.reservation.core.domain.RestaurantMap

import scala.concurrent.Future

trait RestaurantMapRepository {

  def findById(id: String): Future[Option[RestaurantMap]]
  def upsert(id: String, restaurantMap: RestaurantMap): Future[RestaurantMap]
  def delete(id: String): Future[Unit]

}
