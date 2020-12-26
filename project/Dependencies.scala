import sbt._
import sbt.librarymanagement.Configuration

object Dependencies {
  object versions {
    val http4s: String        = "0.21.14"
    val circe: String         = "0.13.0"
    val slf4j: String         = "1.7.28"
    val cats: String          = "2.3.0"

    object test {
      val uTest: String          = "0.7.2"
    }
  }

  val catsCore: ModuleID     = "org.typelevel"    %% "cats-core"            % versions.cats
  val catsKernel: ModuleID   = "org.typelevel"    %% "cats-kernel"          % versions.cats
  val catsEffect: ModuleID   = "org.typelevel"    %% "cats-effect"          % versions.cats
  val circeGeneric: ModuleID = "io.circe"         %% "circe-generic"        % versions.circe
  val circeCore: ModuleID    = "io.circe"         %% "circe-core"           % versions.circe
  val circeLiteral: ModuleID = "io.circe"         %% "circe-literal"        % versions.circe
  val http4sCore: ModuleID   = "org.http4s"       %% "http4s-core"          % versions.http4s
  val http4sCirce: ModuleID  = "org.http4s"       %% "http4s-circe"         % versions.http4s
  val http4sDsl: ModuleID    = "org.http4s"       %% "http4s-dsl"           % versions.http4s
  val http4sServer: ModuleID = "org.http4s"       %% "http4s-blaze-server"  % versions.http4s
  val http4sClient: ModuleID = "org.http4s"       %% "http4s-blaze-client"  % versions.http4s
  val slf4jSimple: ModuleID  = "org.slf4j"        %  "slf4j-simple"         % versions.slf4j
  val slf4jApi: ModuleID     = "org.slf4j"        %  "slf4j-api"            % versions.slf4j

  object test {
    def uTest(configuration: Configuration*): ModuleID = "com.lihaoyi"  %% "utest"                      % versions.test.uTest          % configuration.map(_.name).mkString(", ")
  }
}