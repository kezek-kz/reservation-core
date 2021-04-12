package kezek.reservation.core.codec

import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor}
import kezek.reservation.core.domain.dto.ReservationStateDTO
import kezek.reservation.core.domain.dto.ReservationStateDTO.{ApprovedDTO, CompletedDTO, PaidDTO, PreparingDTO, RejectedDTO}
import kezek.reservation.core.domain.ReservationState.{APPROVED, Approved, COMPLETED, CREATED, Completed, Created, PAID, PREPARING, Paid, Preparing, REJECTED, Rejected}
import ReservationStateDTO.{ApprovedDTO, CompletedDTO, PaidDTO, PreparingDTO, RejectedDTO}
import ReservationStateDTO.{ApprovedDTO, CompletedDTO, PaidDTO, PreparingDTO, RejectedDTO}
import kezek.reservation.core.domain.ReservationState

import scala.util.{Failure, Success}

trait MainCodec extends JodaTimeCodec {

  implicit val reservationStateEncoder: Encoder[ReservationState] = Encoder.instance {
    case s: Created => s.asJson.dropNullValues
    case s: Approved => s.asJson.dropNullValues
    case s: Rejected => s.asJson.dropNullValues
    case s: Paid => s.asJson.dropNullValues
    case s: Preparing => s.asJson.dropNullValues
    case s: Completed => s.asJson.dropNullValues
  }

  implicit val reservationStateDecoder: Decoder[ReservationState] = new Decoder[ReservationState] {
    final def apply(c: HCursor): Decoder.Result[ReservationState] = {
      def code = c.downField("name").as[String].toTry

      code match {
        case Success(s) if s == CREATED => c.as[Created]
        case Success(s) if s == APPROVED => c.as[Approved]
        case Success(s) if s == REJECTED => c.as[Rejected]
        case Success(s) if s == PAID => c.as[Paid]
        case Success(s) if s == PREPARING => c.as[Preparing]
        case Success(s) if s == COMPLETED => c.as[Completed]
        case Failure(exception) => throw exception
        case _ => throw new RuntimeException("Invalid state name")
      }
    }
  }

  implicit val reservationStateDTOEncoder: Encoder[ReservationStateDTO] = Encoder.instance {
    case s: ApprovedDTO => s.asJson.dropNullValues
    case s: RejectedDTO => s.asJson.dropNullValues
    case s: PaidDTO => s.asJson.dropNullValues
    case s: PreparingDTO => s.asJson.dropNullValues
    case s: CompletedDTO => s.asJson.dropNullValues
  }

  implicit val reservationStateDTODecoder: Decoder[ReservationStateDTO] = new Decoder[ReservationStateDTO] {
    final def apply(c: HCursor): Decoder.Result[ReservationStateDTO] = {
      def code = c.downField("name").as[String].toTry

      code match {
        case Success(s) if s == APPROVED => c.as[ApprovedDTO]
        case Success(s) if s == REJECTED => c.as[RejectedDTO]
        case Success(s) if s == PAID => c.as[PaidDTO]
        case Success(s) if s == PREPARING => c.as[PreparingDTO]
        case Success(s) if s == COMPLETED => c.as[CompletedDTO]
        case Failure(exception) => throw exception
        case _ => throw new RuntimeException("Invalid state name")
      }
    }
  }


}
