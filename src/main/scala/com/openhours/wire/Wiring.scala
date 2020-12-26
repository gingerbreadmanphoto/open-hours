package com.openhours.wire

import cats.effect.{ConcurrentEffect, ExitCode, Timer}
import com.openhours.config.Config
import com.openhours.http.ScheduleController
import com.openhours.service.{ScheduleProcessor, ScheduleService}
import com.openhours.utils.Controller
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import fs2.Stream
import org.http4s.implicits._

case class Wiring[F[_]](server: Stream[F, ExitCode])

object Wiring {
  def wiring[
    F[_] : ConcurrentEffect: Timer
  ](config: Config, executionContext: ExecutionContext): Wiring[F] = {
    val scheduleProcessor: ScheduleProcessor = ScheduleProcessor()
    val scheduleService: ScheduleService[F] = ScheduleService[F](scheduleProcessor)
    val scheduleController: Controller[F] = ScheduleController[F](scheduleService)

    val server              = BlazeServerBuilder[F](executionContext)
      .bindHttp(config.port, config.host)
      .withHttpApp(scheduleController.route.orNotFound)
      .withConnectorPoolSize(10)
      .serve

    Wiring(
      server = server
    )
  }
}
