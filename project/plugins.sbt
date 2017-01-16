// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % meta.Dependencies.playVersion)

libraryDependencies ++= Seq(
  "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
)