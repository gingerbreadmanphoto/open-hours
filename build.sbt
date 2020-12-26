import DockerSyntax._

name := "open-hours"

version := "0.1"

scalaVersion := "2.13.4"

lazy val `open-hours` = (project in file("."))
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.catsCore,
      Dependencies.catsKernel,
      Dependencies.catsEffect,
      Dependencies.circeCore,
      Dependencies.circeGeneric,
      Dependencies.circeLiteral,
      Dependencies.http4sCore,
      Dependencies.http4sCirce,
      Dependencies.http4sDsl,
      Dependencies.http4sServer,
      Dependencies.slf4jSimple,
      Dependencies.slf4jApi
    ),
    libraryDependencies ++= Seq(
      Dependencies.test.uTest(Test, IntegrationTest),
      Dependencies.http4sClient
    ),
    Defaults.itSettings,
    testFrameworks := Seq(new TestFramework("utest.runner.Framework"))
  )
  .configs(IntegrationTest)
  .withDocker(exposedPorts = Seq(8090))