scalaVersion := "2.11.8"


libraryDependencies ++= Seq(
  "com.bowlingx" %% "play-webpack-lib" % Option(System.getProperty("plugin.version")).get
)

webpackManifest := Option(file ("conf/webpack-assets.json"))