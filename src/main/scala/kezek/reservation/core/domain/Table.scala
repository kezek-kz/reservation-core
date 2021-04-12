package kezek.reservation.core.domain

case class Table(id: String,
                 slug: String,
                 mapId: String,
                 capacity: Int,
                 state: String,
                 `type`: String,
                 deposit: BigDecimal,
                 qrLink: Option[String])
