package com.openhours

import cats.effect.{ExitCode, IO, IOApp, Resource, SyncIO}
import com.openhours.config.Config
import com.openhours.wire.Wiring
import scala.concurrent.ExecutionContext

object Application extends IOApp.WithContext {
  override protected def executionContextResource: Resource[SyncIO, ExecutionContext] =
    Resource.liftF(SyncIO(scala.concurrent.ExecutionContext.global))

  override def run(args: List[String]): IO[ExitCode] = {
    for {
      wiring <- IO.delay(
        Wiring.wiring[IO](
          config           = Config("0.0.0.0", 8090),
          executionContext = executionContext
        )
      )
      exitCode       <- wiring.server
        .compile
        .drain
        .attempt
        .map {
          case Right(_) => ExitCode.Success
          case Left(_)  => ExitCode.Error
        }

    } yield exitCode
  }
}
