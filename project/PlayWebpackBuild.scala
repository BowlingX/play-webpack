import sbt.Keys._
import sbt._

object PlayWebpackBuild {

  def pluginSettings : Seq[Setting[_]] = {
    libraryDependencies ++= Seq(
      "io.spray" %%  "spray-json" % "1.3.3"
    )
  }

  def commonSettings: Seq[Setting[_]] = {
    Seq(
      scalacOptions ++= Seq(
        "-target:jvm-1.8",
        "-encoding", "UTF-8",
        "-unchecked",
        "-deprecation",
        "-Xfuture",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-numeric-widen",
        "-Ywarn-value-discard",
        "-Ywarn-unused"
      ),
      organization := "com.bowlingx",
      scalaVersion := "2.11.8"
    )
  }

}