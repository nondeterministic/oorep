import sbt.Keys.libraryDependencies

val myScalaVersion     = "2.13.13"
val scalaTestPlusVersion = "5.1.0"
val scalaJsDomVersion  = "2.3.0"
val scalaTagsVersion   = "0.12.0"
val circeVersion       = "0.14.5"
val quillVersion       = "4.8.0"
val pgDriverVersion    = "42.7.1"
val scriptsVersion     = "1.2.0"
val apacheCommonsMailV = "1.5"
val scalarxVersion     = "0.4.3"
val sttpVersion        = "4.0.0-M8"
val monixVersion       = "3.2.1"

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
      "com.lihaoyi" %%% "scalarx" % scalarxVersion,
      "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
      "com.softwaremill.sttp.client4" %%% "core" % sttpVersion,
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
  version := "0.16.1"
)

// loads the frontend project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project backend" :: s}
