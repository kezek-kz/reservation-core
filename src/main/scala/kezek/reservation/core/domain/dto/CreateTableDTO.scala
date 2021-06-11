package kezek.reservation.core.domain.dto

case class CreateTableDTO(id: String,
                          slug: String,
                          capacity: Int,
                          state: String,
                          `type`: String,
                          deposit: BigDecimal)
