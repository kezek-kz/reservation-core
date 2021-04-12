package kezek.reservation.core.domain.dto

case class UpdateTableDTO(slug: String,
                          capacity: Int,
                          state: String,
                          `type`: String,
                          deposit: BigDecimal)
