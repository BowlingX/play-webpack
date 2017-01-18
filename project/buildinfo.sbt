import meta.Dependencies._

enablePlugins(BuildInfoPlugin)

buildInfoKeys := Seq[BuildInfoKey](
  "playVersion" -> playVersion,
  "scala210Version" -> scala210Version,
  "scala211Version" -> scala211Version
)

buildInfoPackage := "com.bowlingx.meta"