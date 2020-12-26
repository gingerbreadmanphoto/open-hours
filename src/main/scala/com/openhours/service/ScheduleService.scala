package com.openhours.service

import cats.Show
import cats.effect.Sync
import com.openhours.domain._
import com.openhours.utils.TimeFormatter
import cats.syntax.functor._
import cats.syntax.show._

trait ScheduleService[F[_]] {
  def prettify(request: Map[WeekDay, List[DailyScheduleCell]]): F[ScheduleResponse]
}

object ScheduleService {
  case class Interval(day: WeekDay, open: Int, close: Int)
  object Interval {
    def openCell(interval: Interval): DailyScheduleCell   = DailyScheduleCell(DailyScheduleCellType.Open, interval.open)
    def closedCell(interval: Interval): DailyScheduleCell = DailyScheduleCell(DailyScheduleCellType.Closed, interval.close)
    implicit val show: Show[Interval] = (interval: Interval) => {
      val openTime  = TimeFormatter.format(interval.open)
      val closeTime = TimeFormatter.format(interval.close)
      s"$openTime - $closeTime"
    }
  }

  def apply[F[_]](processor: ScheduleProcessor)(implicit F: Sync[F]): ScheduleService[F] = new ScheduleService[F] {
    override def prettify(request: Map[WeekDay, List[DailyScheduleCell]]): F[ScheduleResponse] = {

      def printIntervals(intervals: List[Interval]): String = {
        if (intervals.nonEmpty) {
          intervals
            .map(_.show)
            .mkString(", ")
        } else {
          "Closed"
        }
      }

      F.defer(F.fromTry(processor.process(request)))
        .map { result =>
          val groupedSchedule = result.groupBy(_.day)

          WeekDay
            .values
            .map { day =>
              val intervals = groupedSchedule.getOrElse(day, List.empty)

              PrettifiedDailySchedule(day, printIntervals(intervals))
            }
        }
        .map(ScheduleResponse(_))
    }
  }
}