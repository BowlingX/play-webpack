import sbt.Keys._
import sbt._
import com.bowlingx.meta.BuildInfo._

object PlayWebpackBuild {

  def sharedSettings: Seq[Setting[_]] = {
    Seq(
      organization := "com.bowlingx",
      scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-encoding", "UTF-8"
      )
    )
  }

  def pluginSettings: Seq[Setting[_]] = {
    sharedSettings ++ (libraryDependencies ++= Seq(
      "io.spray" %% "spray-json" % "1.3.3"
    ))
  }

  def scala210Project: Seq[Setting[_]] = {
    Seq(
      crossScalaVersions := Seq(scala210Version, scala211Version),
      scalaVersion := scala210Version
    )
  }

  def mainSettings: Seq[Setting[_]] = {
    sharedSettings ++ Seq(
      scalacOptions ++= Seq(
        "-target:jvm-1.8",
        "-Xfuture",
        "-Yno-adapted-args",
        "-Ywarn-dead-code",
        "-Ywarn-numeric-widen",
        "-Ywarn-value-discard",
        "-Ywarn-unused"
      ),
      scalaVersion := scala211Version
    )
  }

}