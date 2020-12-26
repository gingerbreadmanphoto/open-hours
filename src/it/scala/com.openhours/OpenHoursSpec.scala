package com.openhours

import cats.effect.{IO, Resource}
import com.openhours.config.Config
import com.openhours.wire.Wiring
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import utest._
import cats.effect._
import cats.effect.IO._
import io.circe._
import io.circe.literal._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.circe._
import org.http4s.client.dsl.io._
import scala.concurrent.ExecutionContext

object OpenHoursSpec extends TestSuite {
  implicit val timer: Timer[IO]               = IO.timer(ExecutionContext.global)
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  override def tests: Tests = Tests {
    test("it should respond for the http request with the prettified schedule") {
      wireResource(10000)
        .use { client =>
          val request = POST(
            json"""{
                      "monday": [
                        {"type": "open", "value": ${60 * 60 * 10}},
                        {"type": "closed", "value": ${60 * 60 * 19 + 60 * 30}},
                        {"type": "open", "value": ${60 * 60 * 20}},
                        {"type": "closed", "value": ${60 * 60 * 23 + 60 * 30}}
                      ],
                      "wednesday": [{"type": "open", "value": ${60 * 60 * 13}}],
                      "thursday": [{"type": "closed", "value": ${60 * 60 * 5}}],
                      "sunday": []
                  }""",
            uri"http://localhost:10000/schedule/pretty"
          )

          request
            .flatMap(client.expect[Json])
            .flatMap { response =>

              IO.delay(
                response ==> json"""
                  {
                   "result" : {
                     "schedule" : [
                       {
                         "day" : "Monday",
                         "intervals" : "10AM - 7:30PM, 8PM - 11:30PM"
                       },
                       {
                         "day" : "Tuesday",
                         "intervals" : "Closed"
                       },
                       {
                         "day" : "Wednesday",
                         "intervals" : "1PM - 5AM"
                       },
                       {
                         "day" : "Thursday",
                         "intervals" : "Closed"
                       },
                       {
                         "day" : "Friday",
                         "intervals" : "Closed"
                       },
                       {
                         "day" : "Saturday",
                         "intervals" : "Closed"
                       },
                       {
                         "day" : "Sunday",
                         "intervals" : "Closed"
                       }
                     ]
                   },
                   "message" : "OK",
                   "code" : 200
                }
              """
              )
            }
      }
      .unsafeToFuture()
    }

    test("it should respond with an error response") {
      wireResource(10001)
        .use { client =>
          val request = POST(
            json"""{"wednesday": [{"type": "open", "value": ${60 * 60 * 13}}]}""",
            uri"http://localhost:10001/schedule/pretty"
          )

          request
            .flatMap(client.expect[Json])
            .flatMap { response =>

              IO.delay(
                response ==> json"""
                {
                  "message" : "Unclosed interval on wednesday at 46800",
                  "code" : 400
                }
              """
              )
            }
        }
        .unsafeToFuture()
    }
  }

  private def wireResource(port: Int): Resource[IO, Client[IO]] =
    for {
      client <- BlazeClientBuilder[IO](ExecutionContext.global).resource
      wiring <- Resource.liftF(
        IO.delay(
          Wiring.wiring[IO](
            config           = Config("0.0.0.0", port),
            executionContext = ExecutionContext.global
          )
        )
      )
      _    <- Resource.make(wiring.server.compile.drain.start)(_.cancel)
    } yield client
}