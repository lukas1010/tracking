import com.typesafe.sbt.packager.docker.ExecCmd

name := """vehicle-tracking"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala, DockerPlugin, JavaAppPackaging)

scalaVersion := "2.11.7"

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % "1.0.1"
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

scalacOptions in ThisBuild ++= Seq("-feature", "-language:postfixOps")

fork in run := true

dockerBaseImage := "openjdk:jre"

dockerExposedPorts := Seq(7007)

dockerExposedVolumes := Seq("/opt/docker/conf")

dockerCommands := dockerCommands.value.filterNot {

  case ExecCmd("RUN", args @ _*) => args.contains("chown") && args.contains("/opt/docker/conf")

  case ExecCmd("RUN", args @ _*) => args.contains("mkdir") && args.contains("/opt/docker/conf")

  case cmd                       => false

}
