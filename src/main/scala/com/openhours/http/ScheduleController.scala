package com.openhours.http

import cats.effect.Sync
import com.openhours.service.ScheduleService
import com.openhours.utils.Controller
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import io.circe.Decoder._
import cats.syntax.flatMap._
import com.openhours.domain.{DailyScheduleCell, WeekDay}

object ScheduleController {
  def apply[F[_]](service: ScheduleService[F])(implicit F: Sync[F]): Controller[F] = new Controller[F] with Http4sDsl[F] {
    override def route: HttpRoutes[F] = {
      HttpRoutes.of[F] {
        case req @ POST -> Root / "schedule" / "pretty" =>
          Controller.wrapResponse {
            req
              .as[Map[WeekDay, List[DailyScheduleCell]]]
              .flatMap(service.prettify(_))
          }
          .flatMap(Ok(_))
      }
    }
  }
}