import sbt.Keys._
import sbt._
import com.bowlingx.meta.BuildInfo._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.ReleaseStateTransformations._
import xerial.sbt.Sonatype.SonatypeKeys._
import scala.util.Try
import scala.xml.Group
import play.sbt.PlayImport._

object PlayWebpackBuild {

  private[this] val projectStartYear = 2017

  private[this] val sonatypeUsername = Try(
    System.getenv("SONATYPE_USERNAME")
  ).toOption.flatMap(r => Option(r)).getOrElse("")

  private[this] val sonatypePassword = Try(
    System.getenv("SONATYPE_PASSWORD")
  ).toOption.flatMap(r => Option(r)).getOrElse("")

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
      crossScalaVersions := Seq(scala211Version, scala210Version),
      scalaVersion := scala211Version
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
      )
    )
  }

  def playModuleSettings: Seq[Setting[_]] = {
    Seq(
      scalaVersion := scala211Version,
      crossScalaVersions := Seq(scala211Version),
      libraryDependencies += filters,
      libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
      sourceGenerators in Test += task[Seq[File]] {
        val file = (sourceManaged in Test).value / "com" / "bowlingx" / "webpack" / "Manifest.scala"
        val code =
          s"""
             |package com.bowlingx.webpack
             |
       |object WebpackManifest extends WebpackManifestType {
             |  val entries:Map[String, WebpackEntry] = Map(("server" -> WebpackEntry(Some("/assets/scripts/test.js"), None)))
             |}
     """.stripMargin
        IO write(file, code)
        Seq(file)
      }
    )
  }

  def releaseSettings: Seq[Setting[_]] = {
    Seq(
      releaseCrossBuild := false,
      releaseProcess := Seq[ReleaseStep](
        checkSnapshotDependencies,
        inquireVersions,
        runClean,
        releaseStepCommandAndRemaining("test"),
        releaseStepCommandAndRemaining("+publishLocal"),
        releaseStepCommandAndRemaining("+play-webpack-plugin/scripted"),
        setReleaseVersion,
        commitReleaseVersion,
        tagRelease,
        releaseStepCommandAndRemaining("+publishSigned"),
        setNextVersion,
        commitNextVersion,
        releaseStepCommandAndRemaining("sonatypeReleaseAll"),
        pushChanges
      )
    )
  }

  def getCredentials: Credentials = {
    Credentials(
      "Sonatype Nexus Repository Manager",
      "oss.sonatype.org",
      sonatypeUsername,
      sonatypePassword
    )
  }

  def publishSettings: Seq[Setting[_]] = {
    Seq(
      publishMavenStyle := false,
      sonatypeProfileName := sonatypeUsername,
      credentials += getCredentials,
      publishArtifact in Test := false,
      packageOptions <<= (packageOptions, name, version, organization) map {
        (opts, title, version, vendor) =>
          opts :+ Package.ManifestAttributes(
            "Created-By" -> "Simple Build Tool",
            "Built-By" -> System.getProperty("user.name"),
            "Build-Jdk" -> System.getProperty("java.version"),
            "Specification-Title" -> title,
            "Specification-Vendor" -> "David Heidrich",
            "Specification-Version" -> version,
            "Implementation-Title" -> title,
            "Implementation-Version" -> version,
            "Implementation-Vendor-Id" -> vendor,
            "Implementation-Vendor" -> "David Heidrich",
            "Implementation-Url" -> "https://github.com/BowlingX/play-webpack"
          )
      },

      homepage := Some(url("https://github.com/BowlingX/play-webpack")),
      startYear := Some(projectStartYear),
      licenses := Seq(("MIT", url("https://raw.githubusercontent.com/BowlingX/play-webpack/master/LICENSE.md"))),
      pomExtra <<= pomExtra { (pom) =>
        pom ++ Group(
          <scm>
            <connection>scm:git:git://github.com/BowlingX/play-webpack.git</connection>
            <developerConnection>scm:git:git@github.com:BowlingX/play-webpack.git</developerConnection>
            <url>https://github.com/BowlingX/play-webpack</url>
          </scm>
            <developers>
              <developer>
                <id>BowlingX</id>
                <name>David Heidrich</name>
                <url>http://bowlingx.com</url>
              </developer>
            </developers>
        )
      }
    )
  }

}