scalaVersion := "2.11.8"

{
  val pluginVersion = Option(System.getProperty("plugin.version"))
  if (pluginVersion.isEmpty) {
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
  }
  else {
    libraryDependencies ++= Seq(
      "com.bowlingx" %% "play-webpack-lib" % pluginVersion.get
    )
  }
}

webpackManifest := file("conf/webpack-assets.json")