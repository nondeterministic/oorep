import sbt.Keys.libraryDependencies

val myScalaVersion     = "2.13.1"
val scalaTestPlusVersion = "5.1.0"
val scalaJsDomVersion  = "1.0.0"
val scalaTagsVersion   = "0.8.6"
val scalatagsrxVersion = "0.5.0"
val scalaJQueryVersion = "2.0"
val circeVersion       = "0.13.0"
val rosHttpVersion     = "3.0.0"
val quillVersion       = "3.5.2"
val pgDriverVersion    = "42.2.5"
val scriptsVersion     = "1.1.4"
val apacheCommonsMailV = "1.5"

resolvers in ThisBuild += "fr.hmil" at "https://files.hmil.fr/maven"

useJCenter := true

scalaVersion in ThisBuild := myScalaVersion

scalaJSStage in Global := FullOptStage

lazy val backend = (project in file("backend")).settings(commonSettings).settings(
  scalaJSProjects := Seq(frontend, sec_frontend),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  isDevMode in scalaJSPipeline := false,
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
).enablePlugins(PlayScala)
 .dependsOn(sharedJvm)

lazy val frontend = (project in file("frontend")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,

  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
    "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
    "org.querki" %%% "jquery-facade" % scalaJQueryVersion,
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "fr.hmil" %%% "roshttp" % rosHttpVersion,
    "com.timushev" %%% "scalatags-rx" % scalatagsrxVersion
  ),
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
 .dependsOn(sharedJs)

lazy val sec_frontend = (project in file("sec_frontend")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,

  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
    "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
    "org.querki" %%% "jquery-facade" % scalaJQueryVersion,
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "fr.hmil" %%% "roshttp" % rosHttpVersion, 
    "com.timushev" %%% "scalatags-rx" % scalatagsrxVersion
  ),
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
 .dependsOn(sharedJs)

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
      "org.querki" %%% "jquery-facade" % scalaJQueryVersion,
      "fr.hmil" %%% "roshttp" % rosHttpVersion,
      "com.timushev" %%% "scalatags-rx" % scalatagsrxVersion,
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % "test",
      guice,
      specs2 % Test
    ),
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

// This works only if you also do `export SBT_OPTS="-Dconfig.file=backend/conf/application.conf"`
// prior to running sbt!
// val restConfig = ConfigFactory.load()
// val oorepVersion = restConfig.getString("oorep_version")

lazy val commonSettings = Seq(
  scalaVersion := myScalaVersion,
  organization := "org.multics.baueran.frep",
  maintainer := "baueran@gmail.com",
  version := "0.12.0-beta1"
)

// TODO: This doesn't work, and I can't be bothered to get it to work.
// It turns out Akka has default chunk size of 1MB whereas RosHttp only had 8192 bytes.
//
// https://discuss.lightbend.com/t/how-to-adjust-the-max-upload-chunk-size-in-play/2260
// https://www.playframework.com/documentation/2.8.x/ConfigFile
// import PlayKeys._
// PlayKeys.devSettings += "akka.http.parsing.max-chunk-size" -> "8192 b"

// loads the frontend project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project backend" :: s}
