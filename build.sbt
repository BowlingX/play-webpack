import PlayWebpackBuild._

lazy val root = project.in(file(".")).aggregate(lib, sbtPlugin, `play-webpack`).settings(mainSettings).enablePlugins(CrossPerProjectPlugin)

lazy val `play-webpack` = project.in(file("src") / "play-module").dependsOn(lib).settings(mainSettings ++ playModuleSettings ++ publishSettings)

lazy val lib = Project("play-webpack-lib", file("src") / "lib").settings(sharedSettings ++ publishSettings ++ scala210Project:_*)

lazy val sbtPlugin = Project("play-webpack-plugin", file("src") / "sbt-plugin")
  .settings(publishSettings ++ pluginSettings:_*).dependsOn(lib)

