/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

import Dependencies.Versions
import de.heikoseeberger.sbtheader.FileType
import Dependencies.ScalaVersions._

lazy val root = project
  .in(file("."))
  .aggregate(client, plugin, docs)
  .settings(
    name := "play-soap",
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val client = project
  .in(file("client"))
  .enablePlugins(PublishLibrary)
  .settings(
    name := "play-soap-client",
    description := "play-soap client",
    crossScalaVersions := Seq(scala211, scala212, scala213),
    Dependencies.`play-client`,
  )

lazy val plugin = project
  .in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .enablePlugins(PublishSbtPlugin)
  .settings(
    name := "sbt-play-soap",
    organization := "com.typesafe.sbt",
    description := "play-soap sbt plugin",
    Dependencies.plugin,
    crossScalaVersions := Seq(scala212),
    addSbtPlugin("com.typesafe.play" % "sbt-plugin" % Versions.Play),
    resourceGenerators in Compile += generateVersionFile.taskValue,
    scriptedLaunchOpts ++= Seq(
      s"-Dscala.version=${scalaVersion.value}",
      s"-Dscala.crossVersions=${(crossScalaVersions in client).value.mkString(",")}",
      s"-Dproject.version=${version.value}",
      s"-Dcxf.version=${Versions.CXF}",
    ),
    scriptedBufferLog := false,
    scriptedDependencies := (())
  )

lazy val docs = (project in file("docs"))
  .enablePlugins(SbtTwirl)
  .enablePlugins(SbtWeb)
  .settings(
    crossScalaVersions := Seq(scala212),
    headerMappings := headerMappings.value + (FileType("html") -> HeaderCommentStyle.twirlStyleBlockComment),
    headerSources.in(Compile) ++= sources.in(Compile, TwirlKeys.compileTemplates).value,
    WebKeys.pipeline ++= {
      val clientDocs = (mappings in (Compile, packageDoc) in client).value.map { case (file, _name) =>
        file -> ("api/client/" + _name)
      }
      val pluginDocs = (mappings in (Compile, packageDoc) in plugin).value.map { case (file, _name) =>
        file -> ("api/sbtwsdl/" + _name)
      }
      clientDocs ++ pluginDocs
    },
    publish / skip := true
  )

def generateVersionFile =
  Def.task {
    val clientVersion = (version in client).value
    val pluginVersion = version.value
    val file          = (resourceManaged in Compile).value / "play-soap.version.properties"
    val content =
      s"""play-soap-client.version=$clientVersion
         |sbt-play-soap.version=$pluginVersion
     """.stripMargin
    if (!file.exists() || !(IO.read(file) == content)) {
      IO.write(file, content)
    }
    Seq(file)
  }

dynverVTagPrefix in ThisBuild := false
