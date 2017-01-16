import PlayWebpackBuild._

commonSettings

name := "play-react"

scalaSource in Compile := baseDirectory.value / "src" / "main" / "scala"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test

lazy val root = (project in file(".")).dependsOn(lib).aggregate(lib)

lazy val lib = Project("play-react-lib", file("src") / "lib")

lazy val sbtPlugin = Project("play-webpack-plugin", file("src") / "sbt-plugin").settings(pluginSettings:_*).dependsOn(lib).aggregate(lib)