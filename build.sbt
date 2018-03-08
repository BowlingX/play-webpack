import PlayWebpackBuild._

releaseSettings

lazy val root = project.in(file(".")).aggregate(lib, sbtPlugin, `play-webpack`).disablePlugins(ScriptedPlugin)
    .settings(mainSettings ++ Seq(packagedArtifacts := Map.empty, skip in publish := true))

lazy val `play-webpack` = project.in(file("src") / "play-module").disablePlugins(ScriptedPlugin).dependsOn(lib)
    .settings(playModuleSettings ++ publishSettings)

lazy val lib = Project("play-webpack-lib", file("src") / "lib").disablePlugins(ScriptedPlugin)
    .settings(sharedSettings ++ publishSettings ++ librarySettings:_*)

lazy val sbtPlugin = Project("play-webpack-plugin", file("src") / "sbt-plugin")
  .settings(publishSettings ++ pluginSettings:_*).dependsOn(lib)
