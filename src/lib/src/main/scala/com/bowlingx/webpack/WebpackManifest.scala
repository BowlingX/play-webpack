package com.bowlingx.webpack

trait WebpackEntryType {
  val js: Option[String]
  val css: Option[String]
}

trait WebpackManifestType {
  val entries: Map[String, Either[WebpackEntryType, String]]
}

case class WebpackEntry(js: Option[String], css: Option[String]) extends WebpackEntryType

class WebpackManifestContainer(val entries: Map[String, Either[WebpackEntryType, String]]) extends WebpackManifestType

object WebpackManifestContainer {
  def apply(entries:Map[String, Either[WebpackEntryType, String]]) : WebpackManifestContainer = {
    new WebpackManifestContainer(entries)
  }
}