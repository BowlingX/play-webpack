{
  val pluginVersion = Option(System.getProperty("plugin.version"))
  if(pluginVersion.isEmpty) {
    throw new RuntimeException(
      """|The system property 'plugin.version' is not defined.
                                  |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
    }
  else {
    addSbtPlugin("com.bowlingx" % "play-webpack-plugin" % pluginVersion.get)
  }
}