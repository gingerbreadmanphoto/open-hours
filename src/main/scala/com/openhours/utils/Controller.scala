package com.openhours.utils

import cats.effect.Sync
import com.openhours.domain.{Response, ServiceError}
import org.http4s.HttpRoutes
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import scala.util.control.NonFatal

trait Controller[F[_]] {
  def route: HttpRoutes[F]
}

object Controller {
  def wrapResponse[F[_], A](response: F[A])(implicit F: Sync[F]): F[Response[A]] = {
    response.attempt.flatMap {
      case Right(data)             => F.pure(Response.Success(data))
      case Left(err: ServiceError) => F.pure(Response.Failure(err, 400))
      case Left(NonFatal(err))     => F.pure(Response.Failure(err, 500))
      case Left(err)               => F.raiseError(err)
    }
  }
}