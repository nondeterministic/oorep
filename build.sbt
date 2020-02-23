import sbt.Keys.libraryDependencies
import sbtcrossproject.{CrossType, crossProject}

val myScalaVersion     = "2.12.9"
val scalaTestVersion   = "3.1.0"
val scalaJsDomVersion  = "0.9.7"
val scalaTagsVersion   = "0.7.0"
val scalatagsrxVersion = "0.4.0"
val scalaJQueryVersion = "1.2"
val circeVersion       = "0.11.1"
val rosHttpVersion     = "2.2.4"
val quillVersion       = "3.4.3"
val pgDriverVersion    = "42.2.5"
val notifyjsVersion    = "0.1.1"

resolvers in ThisBuild += Resolver.bintrayRepo("hmil", "maven")

lazy val backend = (project in file("backend")).settings(commonSettings).settings(
  scalaJSProjects := Seq(frontend, sec_frontend),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  isDevMode in scalaJSPipeline := false,
  libraryDependencies ++= Seq(
    "com.vmunier" %% "scalajs-scripts" % "1.1.2",
    jdbc,
    evolutions,
    "org.postgresql" % "postgresql" % pgDriverVersion,
    "io.getquill" %% "quill-jdbc" % quillVersion,
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestVersion % "test",
    guice,
    specs2 % Test
  )
  // Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
  // EclipseKeys.preTasks := Seq(compile in Compile)
).enablePlugins(PlayScala)
 .dependsOn(sharedJvm)

lazy val frontend = (project in file("frontend")).settings(commonSettings).settings(
  scalaJSUseMainModuleInitializer := true,
  skip in packageJSDependencies := false,

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
  skip in packageJSDependencies := false,

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
      "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestVersion % "test",
      "com.github.nondeterministic" %%% "scalajs-notifyjs" % notifyjsVersion,
      guice,
      specs2 % Test
    )
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
  version := "0.4.0"
)

// loads the frontend project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project backend" :: s}
