package com.openhours.utils

object TimeFormatter {
  def format(secondsOfDay: Int): String = {
    def hoursWithSuffix(hours: Int): (Int, String) = {
      if (hours == 0) {
        (12, "AM")
      } else if (hours > 0 && hours < 12) {
        (hours, "AM")
      } else if (hours == 12) {
        (12, "PM")
      } else {
        (hours - 12, "PM")
      }
    }

    val hours                  = secondsOfDay / (60 * 60)
    val (hoursFixture, suffix) = hoursWithSuffix(hours)
    val minutes                = (secondsOfDay / 60) - hours * 60

    val time = if (minutes == 0) {
      s"$hoursFixture"
    } else if (minutes < 10) {
      s"$hoursFixture:0$minutes"
    } else {
      s"$hoursFixture:$minutes"
    }

    s"$time$suffix"
  }
}