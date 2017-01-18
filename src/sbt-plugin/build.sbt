name := "play-webpack-plugin"

sbtPlugin := true

scalaVersion := "2.10.6"

crossScalaVersions := Seq("2.10.6")

organization := "com.bowlingx"

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false