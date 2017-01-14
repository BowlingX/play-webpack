name := "play-react"

organization := "com.bowlingx"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.8"

scalaSource in Compile := baseDirectory.value / "src" / "main" / "scala"

libraryDependencies += filters
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
