scalaVersion := "2.12.4"


libraryDependencies ++= Seq(
  "com.bowlingx" %% "play-webpack-lib" % Option(System.getProperty("plugin.version")).get
)

webpackManifest := Seq(file ("conf/webpack-assets.json"))