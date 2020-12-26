import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.{DockerAlias, DockerPlugin}
import com.typesafe.sbt.packager.Keys._
import sbt.Project
import sbt.Keys.name

object DockerSyntax {
  implicit class DockerOps(val prj: Project) extends AnyVal {
    def withDocker(exposedPorts: Seq[Int] = Seq.empty): Project = {
      prj
        .enablePlugins(DockerPlugin, JavaAppPackaging)
        .settings(
          Seq(
            dockerAlias         := DockerAlias(
              registryHost = None,
              username     = Some("gingerdocker"),
              name         = name.value,
              tag          = Some("interview")
            ),
            dockerBaseImage     := "openjdk:11.0.8",
            dockerExposedPorts  := exposedPorts,
            dockerBuildOptions  += "--no-cache"
          )
        )
    }
  }
}