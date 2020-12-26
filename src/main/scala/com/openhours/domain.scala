package com.openhours

import io.circe._
import io.circe.generic.semiauto._

object domain {
  sealed trait WeekDay extends Product with Serializable {
    def name: String
  }
  object WeekDay {

    case object Monday    extends WeekDay { override val name: String = "monday"    }
    case object Tuesday   extends WeekDay { override val name: String = "tuesday"   }
    case object Wednesday extends WeekDay { override val name: String = "wednesday" }
    case object Thursday  extends WeekDay { override val name: String = "thursday"  }
    case object Friday    extends WeekDay { override val name: String = "friday"    }
    case object Saturday  extends WeekDay { override val name: String = "saturday"  }
    case object Sunday    extends WeekDay { override val name: String = "sunday"    }

    val values: List[WeekDay] = List(
      Monday,
      Tuesday,
      Wednesday,
      Thursday,
      Friday,
      Saturday,
      Sunday
    )

    implicit val encoder: Encoder[WeekDay]    = Encoder.instance(d => Json.fromString(d.name.capitalize))
    implicit val decoder: KeyDecoder[WeekDay] = KeyDecoder.instance { str =>
      str.toLowerCase match {
        case Monday.   `name` => Some(Monday)
        case Tuesday.  `name` => Some(Tuesday)
        case Wednesday.`name` => Some(Wednesday)
        case Thursday. `name` => Some(Thursday)
        case Friday.   `name` => Some(Friday)
        case Saturday. `name` => Some(Saturday)
        case Sunday.   `name` => Some(Sunday)
        case            name  => None
      }
    }
  }

  sealed trait DailyScheduleCellType extends Product with Serializable {
    def isOpen: Boolean   = false
    def isClosed: Boolean = false
    def name: String
  }
  object DailyScheduleCellType {
    case object Open extends DailyScheduleCellType   {
      override val name: String    = "open"
      override val isOpen: Boolean = true
    }
    case object Closed extends DailyScheduleCellType {
      override val name: String      = "closed"
      override val isClosed: Boolean = true
    }

    implicit val decoder: Decoder[DailyScheduleCellType] = Decoder.decodeString.emap { str =>
      str.toLowerCase match {
        case Open.  `name` => Right(Open)
        case Closed.`name` => Right(Closed)
        case name          => Left(s"$name is a wrong type")
      }
    }
  }

  case class DailyScheduleCell(`type`: DailyScheduleCellType, value: Int)
  object DailyScheduleCell {
    implicit val decoder: Decoder[DailyScheduleCell] = deriveDecoder[DailyScheduleCell]
  }

  case class ScheduleResponse(schedule: List[PrettifiedDailySchedule])
  object ScheduleResponse {
    implicit val encoder: Encoder[ScheduleResponse] = deriveEncoder[ScheduleResponse]
  }

  case class PrettifiedDailySchedule(day: WeekDay, intervals: String)
  object PrettifiedDailySchedule {
    implicit val encoder: Encoder[PrettifiedDailySchedule] = deriveEncoder[PrettifiedDailySchedule]
  }

  sealed trait Response[+A] {
    def message: String
    def code: Int
  }
  object Response {
    case class Success[A] private (result: A, message: String, code: Int) extends Response[A]
    object Success {
      def apply[A](result: A): Response[A] = {
        Success(result, "OK", 200)
      }

      implicit def encoder[A: Encoder]: Encoder[Success[A]] = deriveEncoder[Success[A]]
    }
    case class Failure private (message: String, code: Int) extends Response[Nothing]
    object Failure {
      def apply(failure: Throwable, code: Int): Response[Nothing] = {
        Failure(failure.getMessage, code)
      }

      implicit val encoder: Encoder[Failure] = deriveEncoder[Failure]
    }

    implicit def encoder[A](implicit aEncoder: Encoder[A]): Encoder[Response[A]] = Encoder.instance {
      case r:Success[A] => Success.encoder(aEncoder)(r)
      case r: Failure   => Failure.encoder(r)
    }
  }

  sealed abstract class ServiceError(val message: String) extends RuntimeException(message)
  object ServiceError {

    case class WrongIntervalBeginningError(day: WeekDay)
      extends ServiceError(s"Expected Open for the beginning of new interval. Day ${day.name}")

    case class WrongIntervalEndingError(day: WeekDay)
      extends ServiceError(s"Expected Closed after the Open time. Day ${day.name}")

    case class UnclosedIntervalError(day: WeekDay, open: Int)
      extends ServiceError(s"Unclosed interval on ${day.name} at $open")

    case class OverlappingCellsError(firstDay: WeekDay,
                                     firstCell: DailyScheduleCell,
                                     secondDay: WeekDay,
                                     secondCell: DailyScheduleCell)
      extends ServiceError(s"Cell ${firstCell.`type`}: ${firstCell.value} on ${firstDay.name} and ${secondCell.`type`}: ${secondCell.value} on ${secondDay.name} are overlapping")

    implicit val encoder: Encoder[ServiceError] = Encoder.instance(e => Json.fromString(e.message))
  }
}