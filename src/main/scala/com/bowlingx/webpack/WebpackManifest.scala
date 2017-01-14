package com.bowlingx.webpack


case class WebpackManifest(entries: Map[String, WebpackEntry])

case class WebpackEntry(js: Option[String], css: Option[String])
