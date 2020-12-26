package com.openhours.service

import cats.effect.IO
import com.openhours.domain._
import utest._

object ScheduleServiceSpec extends TestSuite {
  override def tests: Tests = Tests {
    test("prettify should return intervals for defined days and Closed for undefined ones in sorted order") {
      val request = Map(
        WeekDay.Friday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open,    60 * 60 * 11),
          DailyScheduleCell(DailyScheduleCellType.Closed,  60 * 60 * 13),
          DailyScheduleCell(DailyScheduleCellType.Open,    60 * 60 * 15),
          DailyScheduleCell(DailyScheduleCellType.Closed,  60 * 60 * 22)
        ),
        WeekDay.Wednesday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open,    60 * 60 * 12),
          DailyScheduleCell(DailyScheduleCellType.Closed,  60 * 60 * 18)
        )
      )

      service.prettify(request).flatMap { response =>
        IO.delay(
          response ==> ScheduleResponse(
            List(
              PrettifiedDailySchedule(WeekDay.Monday,    "Closed"),
              PrettifiedDailySchedule(WeekDay.Tuesday,   "Closed"),
              PrettifiedDailySchedule(WeekDay.Wednesday, "12PM - 6PM"),
              PrettifiedDailySchedule(WeekDay.Thursday,  "Closed"),
              PrettifiedDailySchedule(WeekDay.Friday,    "11AM - 1PM, 3PM - 10PM"),
              PrettifiedDailySchedule(WeekDay.Saturday,  "Closed"),
              PrettifiedDailySchedule(WeekDay.Sunday,    "Closed")
            )
          )
        )
      }
      .unsafeToFuture()
    }
  }

  private val processor: ScheduleProcessor = ScheduleProcessor()
  private val service: ScheduleService[IO] = ScheduleService[IO](processor)
}