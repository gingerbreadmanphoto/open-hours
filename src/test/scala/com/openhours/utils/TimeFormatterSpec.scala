package com.openhours.utils

import utest._

object TimeFormatterSpec extends TestSuite {
  override def tests: Tests = Tests {
    test("print should format 60 * 60 * 10 to 10AM") {
      TimeFormatter.format(60 * 60 * 10) ==> "10AM"
    }

    test("print should format 60 * 60 * 13 to 1PM") {
      TimeFormatter.format(60 * 60 * 13) ==> "1PM"
    }

    test("print should format 0 to 12AM") {
      TimeFormatter.format(0) ==> "12AM"
    }

    test("print should format 60 * 60 * 12 to 12PM") {
      TimeFormatter.format(60 * 60 * 12) ==> "12PM"
    }

    test("print should add leading zero for minutes less than 10 (60 * 60 * 12 + 60 to 12:01PM)") {
      TimeFormatter.format(60 * 60 * 12 + 60) ==> "12:01PM"
    }

    test("print should not add leading zero for minutes greater than 10 (60 * 60 * 12 + 60 * 10 to 12:10PM)") {
      TimeFormatter.format(60 * 60 * 12 + 60 * 10) ==> "12:10PM"
    }
  }
}