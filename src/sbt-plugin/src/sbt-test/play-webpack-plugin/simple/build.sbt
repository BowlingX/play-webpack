scalaVersion := "2.11.8"

{
  val pluginVersion = System.getProperty("plugin.version")
  if (pluginVersion == null) {
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  }
  else {
    libraryDependencies ++= Seq(
      "com.bowlingx" %% "play-webpack-lib" % "0.1.0-SNAPSHOT"
    )
  }
}

webpackManifest := file("conf/webpack-assets.json")