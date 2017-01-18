import PlayWebpackBuild._

mainSettings

name := "play-webpack"

scalaSource in Compile := baseDirectory.value / "src" / "main" / "scala"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test

lazy val lib = Project("play-webpack-lib", file("src") / "lib").settings(sharedSettings:_*)

lazy val sbtPlugin = Project("play-webpack-plugin", file("src") / "sbt-plugin")
  .settings(pluginSettings:_*).dependsOn(lib).aggregate(lib)

lazy val playWebpack = Project("play-webpack", file(".")).dependsOn(lib).aggregate(lib)

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