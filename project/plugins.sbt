// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % meta.Dependencies.playVersion)

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.4")

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

libraryDependencies ++= Seq(
  "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
)