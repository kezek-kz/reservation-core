package kezek.reservation.core.domain

case class Table(id: String,
                 slug: String,
                 mapId: String,
                 capacity: Int,
                 state: String,
                 `type`: String,
                 deposit: BigDecimal,
                 qrLink: Option[String])

object TableState {
  final val BLOCKED: String = "BLOCKED"
  final val RESERVED: String = "RESERVED"
  final val FREE: String = "FREE"
}
