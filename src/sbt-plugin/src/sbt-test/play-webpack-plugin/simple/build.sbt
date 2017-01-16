scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.bowlingx" %%  "play-webpack-lib" % "0.1.0-SNAPSHOT"
)
webpackManifest := file("conf/webpack-assets.json")