import sbt.Keys.libraryDependencies

val myScalaVersion     = "3.5.2"
val scalaTestPlusVersion = "7.0.1"
val scalaJsDomVersion  = "2.3.0"
val scalaTagsVersion   = "0.13.1"
val circeVersion       = "0.14.10"
val quillVersion       = "4.8.5"
val pgDriverVersion    = "42.7.4"
val scriptsVersion     = "1.3.0"
val apacheCommonsMailV = "1.5"
val sttpVersion        = "4.0.0-M19"
val monixVersion       = "3.4.1"

useJCenter := true

ThisBuild / scalaVersion := myScalaVersion
ThisBuild / evictionErrorLevel := Level.Info

scalaJSStage in Global := FullOptStage

lazy val backend = (project in file("backend")).settings(commonSettings).settings(
  scalaJSProjects := Seq(frontend, sec_frontend),
  Assets / pipelineStages := Seq(scalaJSPipeline, gzip),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
  scalaJSStage := FullOptStage,

  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % scriptsVersion,
    jdbc,
    evolutions,
    "org.postgresql" % "postgresql" % pgDriverVersion,
    "org.apache.commons" % "commons-email" % apacheCommonsMailV,
    "io.getquill" %% "quill-jdbc" % quillVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % "test",
    guice,
    specs2 % Test
  )
).enablePlugins(PlayScala, SbtWeb)
 .dependsOn(shared.jvm)

lazy val frontend = (project in file("frontend")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,
  scalaJSStage := FullOptStage,

  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
    "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "com.softwaremill.sttp.client4" %%% "core" % sttpVersion
  ),
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
 .dependsOn(shared.js)

lazy val sec_frontend = (project in file("sec_frontend")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,

  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
    "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "com.softwaremill.sttp.client4" %%% "core" % sttpVersion
  ),
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
 .dependsOn(shared.js)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
      "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
      "com.softwaremill.sttp.client4" %%% "core" % sttpVersion,
      "com.raquo" %%% "laminar" % "16.0.0",
      "io.monix" %%% "monix" % monixVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % "test",
      guice,
      specs2 % Test
    ),
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val commonSettings = Seq(
  scalaVersion := myScalaVersion,
  organization := "org.multics.baueran.frep",
  maintainer := "baueran@gmail.com",
  version := scala.io.Source.fromFile("./backend/conf/version.txt").mkString.filter(_ >= ' ')
)

// loads the frontend project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project backend" :: s}
