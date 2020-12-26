package com.openhours.service

import com.openhours.domain.{DailyScheduleCell, DailyScheduleCellType, WeekDay}
import com.openhours.domain.ServiceError.{OverlappingCellsError, UnclosedIntervalError, WrongIntervalBeginningError, WrongIntervalEndingError}
import com.openhours.service.ScheduleProcessor.Schedule
import com.openhours.service.ScheduleService.Interval
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

trait ScheduleProcessor {
  def process(schedule: Schedule): Try[List[Interval]]
}

object ScheduleProcessor {
  sealed trait DailyScheduleState
  object DailyScheduleState {
    case object Empty extends DailyScheduleState
    case class OpenTime(day: WeekDay, open: Int) extends DailyScheduleState
    case class PreviousDayOpenTime(day: WeekDay, open: Int) extends DailyScheduleState
    case class ClosedTime(interval: Interval) extends DailyScheduleState
  }

  private type StatefulIntervals = (ListBuffer[Interval], DailyScheduleState)
  private type Schedule          = Map[WeekDay, List[DailyScheduleCell]]

  def apply(): ScheduleProcessor = new ScheduleProcessor {

    override def process(schedule: Schedule): Try[List[Interval]] = {
      val zero: Try[StatefulIntervals] = Success(ListBuffer.empty[Interval] -> DailyScheduleState.Empty)

      def processSchedule(fixturedSchedule: Schedule): Try[StatefulIntervals] = {
        WeekDay
          .values
          .map(day => day -> fixturedSchedule.getOrElse(day, List.empty))
          .foldLeft(zero) {
            case (Success((acc, nextState)), (day, cells)) =>
              val processedDay = processDay(nextState, day, cells)

              processedDay match {
                case Success((values, nextDayState)) => Success(acc.addAll(values) -> nextDayState)
                case Failure(failure)                => Failure(failure)
              }
            case (Failure(failure), _) => Failure(failure)
          }
      }

      val fixturedSchedule = fixtureSchedule(schedule)
      processSchedule(fixturedSchedule).flatMap {
        case (_, s:DailyScheduleState.PreviousDayOpenTime) => Failure(UnclosedIntervalError(s.day, s.open))
        case (intervals, _)                                => Success(intervals.toList)
      }
    }

    /*
      in case if we have the following schedule (where the restaurant's shift starts on Sunday and finishes on Monday):
      -----------------------------------------------------
      monday [{closed}, ..., {}],...,sunday [{},...,{open}]
      -----------------------------------------------------
      we should take monday's first cell and put it to the end of the sunday's schedule
    */
    private def fixtureSchedule(schedule: Schedule): Schedule = {
      val mondaySchedule = schedule.getOrElse(WeekDay.Monday, List.empty)
      val sundaySchedule = schedule.getOrElse(WeekDay.Sunday, List.empty)

      val lastSundayCellOpen    = sundaySchedule.lastOption.filter(_.`type`.isOpen)
      val firstMondayCellClosed = mondaySchedule.headOption.filter(_.`type`.isClosed)

      (lastSundayCellOpen, firstMondayCellClosed) match {
        case (Some(sunday), Some(monday)) =>
          schedule
            .updated(WeekDay.Monday, mondaySchedule.tail)
            .updated(WeekDay.Sunday, sundaySchedule ::: List(monday))
        case _                            => schedule
      }
    }

    private def processDay(initialState: DailyScheduleState,
                           day: WeekDay,
                           cells: List[DailyScheduleCell]): Try[StatefulIntervals] = {

      @tailrec
      def go(cells: List[DailyScheduleCell],
             state: DailyScheduleState,
             result: ListBuffer[Interval]): Try[StatefulIntervals] = {

        cells match {
          case cell :: tail =>         (state, cell.`type`) match {
            case (DailyScheduleState.Empty, DailyScheduleCellType.Open)                     =>
              go(
                tail,
                DailyScheduleState.OpenTime(day, cell.value),
                result
              )
            case (s: DailyScheduleState.OpenTime, DailyScheduleCellType.Closed)             =>
              val interval = Interval(s.day, s.open, cell.value)
              go(
                tail,
                DailyScheduleState.ClosedTime(interval),
                result.append(interval)
              )
            case (s: DailyScheduleState.PreviousDayOpenTime, DailyScheduleCellType.Closed)  =>
              val interval = Interval(s.day, s.open, cell.value)
              go(
                tail,
                DailyScheduleState.ClosedTime(interval),
                result.append(interval)
              )
            case (DailyScheduleState.ClosedTime(prevInterval), DailyScheduleCellType.Open)  =>
              if (prevInterval.close < cell.value) {
                go(
                  tail,
                  DailyScheduleState.OpenTime(day, cell.value),
                  result
                )
              } else {
                Failure(OverlappingCellsError(prevInterval.day, Interval.closedCell(prevInterval), day, cell))
              }

            case (DailyScheduleState.Empty,                  DailyScheduleCellType.Closed) => Failure(WrongIntervalBeginningError(day))
            case (DailyScheduleState.ClosedTime(_),          DailyScheduleCellType.Closed) => Failure(WrongIntervalBeginningError(day))
            case (_: DailyScheduleState.OpenTime,            DailyScheduleCellType.Open)   => Failure(WrongIntervalEndingError(day))
            case (_: DailyScheduleState.PreviousDayOpenTime, DailyScheduleCellType.Open)   => Failure(WrongIntervalEndingError(day))
          }
          case Nil          =>         state match {
            case DailyScheduleState.Empty                  => Success(result -> DailyScheduleState.Empty)
            case DailyScheduleState.ClosedTime(_)          => Success(result -> DailyScheduleState.Empty)
            case s: DailyScheduleState.OpenTime            => Success(result -> DailyScheduleState.PreviousDayOpenTime(s.day, s.open))
            case s: DailyScheduleState.PreviousDayOpenTime => Failure(UnclosedIntervalError(s.day, s.open))
          }
        }
      }

      go(cells, initialState, ListBuffer.empty)
    }
  }
}