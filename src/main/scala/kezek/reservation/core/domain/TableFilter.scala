package kezek.reservation.core.domain

trait TableFilter

object TableFilter {

  case class ByTableIdListFilter(tableIds: Seq[String]) extends TableFilter

  case class ByStateFilter(state: String) extends TableFilter

}
