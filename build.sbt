import PlayWebpackBuild._
import scala.sys.process._

releaseSettings

lazy val root = project.in(file(".")).aggregate(lib, sbtPlugin, `play-webpack`).disablePlugins(ScriptedPlugin)
    .settings(mainSettings ++ Seq(packagedArtifacts := Map.empty, skip in publish := true))

lazy val `play-webpack` = project.in(file("src") / "play-module").disablePlugins(ScriptedPlugin).dependsOn(lib)
    .settings(playModuleSettings ++ publishSettings)

lazy val lib = Project("play-webpack-lib", file("src") / "lib").disablePlugins(ScriptedPlugin)
    .settings(sharedSettings ++ publishSettings:_*)

lazy val sbtPlugin = Project("play-webpack-plugin", file("src") / "sbt-plugin")
  .settings(publishSettings ++ pluginSettings:_*).dependsOn(lib)

lazy val deletePublishedSnapshots = taskKey[Unit]("Deletes published snapshots on sonatype")

deletePublishedSnapshots := {
  Process(
    "curl" :: "--request" :: "DELETE" :: "--write" :: "%{http_code} %{url_effective}\\n" ::
      "--user" :: s"${System.getenv().get("SONATYPE_USERNAME")}:${System.getenv().get("SONATYPE_PASSWORD")}" ::
      "--output" :: "/dev/null" :: "--silent" ::
      s"${Opts.resolver.sonatypeSnapshots.root}/${organization.value.replace(".", "/")}/" :: Nil) ! streams.value.log
}
