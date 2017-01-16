package com.bowlingx

import java.net.URL
import java.nio.file.Paths

import com.bowlingx.playframework.ScriptActionBuilder
import com.bowlingx.providers.{ScriptActionProvider, ScriptResources}
import com.bowlingx.webpack._
import play.api.{Configuration, Environment, Logger}
import play.api.inject.{Binding, Module}
import javax.inject.Singleton

import scala.util.Try

class ReactModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val logger = Logger(this.getClass)

    // read manifest file and preload resources
    val manifest = configuration.getString("webpack.manifestFile").flatMap(environment.resourceAsStream)

    if (manifest.isEmpty) {
      logger.error(
        "Could not find webpack JSON manifest file, make sure to define webpack.manifestFile to an existing path."
      )

    }
    val manifestOption = Try(
      environment.classLoader.loadClass("com.bowlingx.webpack.WebpackManifest").asInstanceOf[WebpackManifestType]
    ).toOption

    val publicToServerEntry = this.mapServerPath(configuration, manifestOption)

    val prependedBundles = configuration.getStringList("webpack.prependBundles").map(list =>
      publicToServerEntry.map(entry => entry.entries.filter(
        r => list.contains(r._1)).toSeq.sortBy(r => list.indexOf(r._1))
      ).getOrElse(Seq.empty)
    ).getOrElse(Seq.empty)

    val engines = this.createEngines(environment, publicToServerEntry, prependedBundles)

    val webpackManifestBinding = Seq(manifestOption.map { manifest =>
      bind(classOf[WebpackManifestType]).to(manifest).in(classOf[Singleton])
    }).flatten

    engines ++ webpackManifestBinding
  }

  /**
    * Creates Engines, based on the bundles defined in the manifest file
    *
    * @param environment         DI
    * @param publicToServerEntry all entries
    * @param prependedBundles    bundles that should be prepended
    * @return engines
    */
  def createEngines(
                     environment: Environment,
                     publicToServerEntry: Option[WebpackManifestType],
                     prependedBundles: Seq[(String, WebpackEntryType)]
                   ): Seq[Binding[ScriptActionBuilder]] = {
    val engines = publicToServerEntry.map(manifest => {
      manifest.entries.filter(e => !prependedBundles.contains(e)).map(entry => {
        bind(classOf[ScriptActionBuilder]).qualifiedWith(entry._1).to(
          new ScriptActionProvider(
            ScriptResources(this.createVendorResources(environment, Some(entry._2), prependedBundles))
          )
        ).in(classOf[Singleton])
      }).toSeq
    }).getOrElse(Seq.empty)

    // default engine that just contains the prepended entries
    engines :+ bind(classOf[ScriptActionBuilder]).to(
      new ScriptActionProvider(ScriptResources(this.createVendorResources(environment, None, prependedBundles)))
    ).in(classOf[Singleton])
  }

  def createVendorResources(
                             environment: Environment,
                             entry: Option[WebpackEntryType],
                             prepends: Seq[(String, WebpackEntryType)]
                           ): Seq[URL] = {
    // we watch the real source in develop, this is faster instead of waiting till other watch processes copy the file to resources
    val preSources = prepends.flatMap(_._2.js.map(
      r => this.getFileFromResourcesOrProjectPath(environment, r))
    ).flatten
    val entrySource = Seq(entry.flatMap(_.js.flatMap(
      r => this.getFileFromResourcesOrProjectPath(environment, r)))
    ).flatten

    preSources ++ entrySource
  }

  def getFileFromResourcesOrProjectPath(env: Environment, file: String): Option[URL] = {
    env.mode match {
      case play.api.Mode.Dev => env.getExistingFile(file).map(_.toURI.toURL)
      case _ => env.resource(file)
    }
  }

  /**
    * Maps the public server path manifest to the internal resource files
    *
    * @param configuration play configuration
    * @param manifest      original manifest
    * @return
    */
  def mapServerPath(configuration: Configuration, manifest: Option[WebpackManifestType]): Option[WebpackManifestType] = {
    manifest.map { manifest =>
      val publicPathOption = configuration.getString("webpack.publicPath")
      val serverPathOption = configuration.getString("webpack.serverPath")
      WebpackManifestContainer((publicPathOption, serverPathOption) match {
        case (Some(publicPath), Some(serverPath)) =>
          manifest.entries.mapValues { case (entry: WebpackEntry) =>
            val jsEntries = entry.js.map(path => Paths.get(serverPath, replacePublicPath(path, publicPath)).toString)
            val cssEntries = entry.css.map(path => Paths.get(serverPath, replacePublicPath(path, publicPath)).toString)
            WebpackEntry(jsEntries, cssEntries)
          }
        case _ => Map.empty[String, WebpackEntry]
      })
    }
  }

  def replacePublicPath(filePath: String, publicPath: String): String = {
    Paths.get(publicPath).relativize(Paths.get(filePath)).toString
  }
}