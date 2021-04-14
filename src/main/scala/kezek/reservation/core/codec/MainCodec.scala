package kezek.reservation.core.codec

import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, HCursor}
import kezek.reservation.core.domain.ReservationState
import kezek.reservation.core.domain.ReservationState._
import kezek.reservation.core.domain.dto.ReservationStateDTO
import kezek.reservation.core.domain.dto.ReservationStateDTO.{CanceledDTO, ReservedDTO, WaitingPaymentDTO}

import scala.util.{Failure, Success}

trait MainCodec extends JodaTimeCodec {

  implicit val reservationStateEncoder: Encoder[ReservationState] = Encoder.instance {
    case s: Created => s.asJson.dropNullValues
    case s: WaitingPayment => s.asJson.dropNullValues
    case s: Canceled => s.asJson.dropNullValues
    case s: Reserved => s.asJson.dropNullValues
  }

  implicit val reservationStateDecoder: Decoder[ReservationState] = new Decoder[ReservationState] {
    final def apply(c: HCursor): Decoder.Result[ReservationState] = {
      def code = c.downField("name").as[String].toTry

      code match {
        case Success(s) if s == CREATED => c.as[Created]
        case Success(s) if s == WAITING_PAYMENT => c.as[WaitingPayment]
        case Success(s) if s == CANCELED => c.as[Canceled]
        case Success(s) if s == RESERVED => c.as[Reserved]
        case Failure(exception) => throw exception
        case _ => throw new RuntimeException("Invalid state name")
      }
    }
  }

  implicit val reservationStateDTOEncoder: Encoder[ReservationStateDTO] = Encoder.instance {
    case s: CanceledDTO => s.asJson.dropNullValues
    case s: ReservedDTO => s.asJson.dropNullValues
    case s: WaitingPaymentDTO => s.asJson.dropNullValues
  }

  implicit val reservationStateDTODecoder: Decoder[ReservationStateDTO] = new Decoder[ReservationStateDTO] {
    final def apply(c: HCursor): Decoder.Result[ReservationStateDTO] = {
      def code = c.downField("name").as[String].toTry

      code match {
        case Success(s) if s == WAITING_PAYMENT => c.as[WaitingPaymentDTO]
        case Success(s) if s == CANCELED => c.as[CanceledDTO]
        case Success(s) if s == RESERVED => c.as[ReservedDTO]
        case Failure(exception) => throw exception
        case _ => throw new RuntimeException("Invalid state name")
      }
    }
  }


}
