package com.openhours.service

import com.openhours.domain.ServiceError.{OverlappingCellsError, UnclosedIntervalError, WrongIntervalBeginningError, WrongIntervalEndingError}
import com.openhours.domain.{DailyScheduleCell, DailyScheduleCellType, WeekDay}
import com.openhours.service.ScheduleService.Interval
import utest._
import scala.util.{Failure, Success}

object ScheduleProcessorSpec extends TestSuite {
  override def tests: Tests = Tests {
    test("process should return a WrongIntervalBeginningError for the Closed cell without the preceding Open one") {
      val request = Map(
        monday -> List(
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 10),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 13),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 22)
        )
      )

      val result = processor.process(request)

      result.isFailure ==> true
      val Failure(error) = result
      error ==> WrongIntervalBeginningError(WeekDay.Monday)
    }

    test("process should return a WrongIntervalEndingError for the Open cell without the Closed one after in the same day") {
      val request = Map(
        monday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 10),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 13),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 22)
        )
      )

      val result = processor.process(request)

      result.isFailure ==> true
      val Failure(error) = result
      error ==> WrongIntervalEndingError(WeekDay.Monday)
    }

    test("process should return a WrongIntervalEndingError for the Open cell without the Closed one after in the previous day") {
      val request = Map(
        monday  -> List(
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 22)
        ),
        tuesday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 22)
        )
      )

      val result = processor.process(request)

      result.isFailure ==> true
      val Failure(error) = result
      error ==> WrongIntervalEndingError(WeekDay.Tuesday)
    }

    test("process should return a UnclosedIntervalError for an empty interval after the Open one") {
      val request = Map(
        monday  -> List(
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 22)
        ),
        tuesday -> List(),
        wednesday -> List(
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 22)
        )
      )

      val result = processor.process(request)

      result.isFailure ==> true
      val Failure(error) = result
      error ==> UnclosedIntervalError(WeekDay.Monday, 60 * 60 * 22)
    }

    test("process should return a UnclosedIntervalError for a Monday's Open interval after the Sunday's Open one") {
      val request = Map(
        monday  -> List(
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 5)
        ),
        sunday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 22)
        )
      )

      val result = processor.process(request)

      result.isFailure ==> true
      val Failure(error) = result
      error ==> UnclosedIntervalError(WeekDay.Monday, 60 * 60 * 5)
    }

    test("process should return a OverlappingCellsError if the close cell overlaps with the open one") {
      val request = Map(
        monday  -> List(
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 15),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 21),
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 17)
        )
      )

      val result = processor.process(request)

      result.isFailure ==> true
      val Failure(error) = result
      error ==> OverlappingCellsError(
        WeekDay.Monday,
        DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 21),
        WeekDay.Monday,
        DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 17)
      )
    }

    test("process should return a UnclosedIntervalError if a day finishes with the Open cell without ") {
      val request = Map(
        monday  -> List(
          DailyScheduleCell(DailyScheduleCellType.Open, 60 * 60 * 15)
        )
      )

      val result = processor.process(request)

      result.isFailure ==> true
      val Failure(error) = result
      error ==> UnclosedIntervalError(WeekDay.Monday, 60 * 60 * 15)
    }

    test("process should return intervals without overnight") {
      val request = Map(
        monday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 10),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 13),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 15),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 22)
        ),
        tuesday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 9),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 12),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 14),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 21)
        )
      )

      val result = processor.process(request)

      result.isSuccess ==> true
      val Success(schedule) = result
      schedule ==> List(
        Interval(WeekDay.Monday,  60 * 60 * 10, 60 * 60 * 13),
        Interval(WeekDay.Monday,  60 * 60 * 15, 60 * 60 * 22),
        Interval(WeekDay.Tuesday, 60 * 60 * 9,  60 * 60 * 12),
        Interval(WeekDay.Tuesday, 60 * 60 * 14, 60 * 60 * 21)
      )
    }

    test("process should return intervals with overnight") {
      val request = Map(
        monday -> List(
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 10),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 13),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 15)
        ),
        tuesday -> List(
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 2),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 14),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 21),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 22)
        ),
        wednesday -> List(
          DailyScheduleCell(DailyScheduleCellType.Closed,   60 * 60 * 2)
        )
      )

      val result = processor.process(request)

      result.isSuccess ==> true
      val Success(schedule) = result
      schedule ==> List(
        Interval(WeekDay.Monday,  60 * 60 * 10, 60 * 60 * 13),
        Interval(WeekDay.Monday,  60 * 60 * 15, 60 * 60 * 2),
        Interval(WeekDay.Tuesday, 60 * 60 * 14, 60 * 60 * 21),
        Interval(WeekDay.Tuesday, 60 * 60 * 22, 60 * 60 * 2)
      )
    }

    test("process should return intervals with overnight if the last cell on Sunday is Open and first cell on Monday is Closed") {
      val request = Map(
        sunday  -> List(
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 10),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 22),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 23)
        ),
        monday -> List(
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 8),
          DailyScheduleCell(DailyScheduleCellType.Open,   60 * 60 * 12),
          DailyScheduleCell(DailyScheduleCellType.Closed, 60 * 60 * 20)
        )
      )

      val result = processor.process(request)

      result.isSuccess ==> true
      val Success(schedule) = result
      schedule ==> List(
        Interval(WeekDay.Monday,  60 * 60 * 12,  60 * 60 * 20),
        Interval(WeekDay.Sunday,  60 * 60 * 10,  60 * 60 * 22),
        Interval(WeekDay.Sunday,  60 * 60 * 23,  60 * 60 * 8)
      )
    }
  }

  private val processor: ScheduleProcessor = ScheduleProcessor()
  val monday: WeekDay                      = WeekDay.Monday
  val tuesday: WeekDay                     = WeekDay.Tuesday
  val wednesday: WeekDay                   = WeekDay.Wednesday
  val sunday: WeekDay                      = WeekDay.Sunday
}