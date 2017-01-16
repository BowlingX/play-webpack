import meta.Dependencies._

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey]("playVersion" -> playVersion)

buildInfoPackage := "com.bowlingx.meta"