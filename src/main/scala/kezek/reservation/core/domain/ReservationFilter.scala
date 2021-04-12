package kezek.reservation.core.domain

trait ReservationFilter

object ReservationFilter {

  case class ByCategoryIdFilter(categoryId: String) extends ReservationFilter
  case class ByTitleFilter(title: String) extends ReservationFilter
  case class ByDescriptionFilter(description: String) extends ReservationFilter
}
