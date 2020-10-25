import sbt.Keys.libraryDependencies

val myScalaVersion     = "2.13.1"
val scalaTestVersion   = "5.1.0"
val scalaJsDomVersion  = "1.0.0"
val scalaTagsVersion   = "0.8.6"
val scalatagsrxVersion = "0.5.0"
val scalaJQueryVersion = "2.0"
val circeVersion       = "0.13.0"
val rosHttpVersion     = "3.0.0"
val quillVersion       = "3.5.2"
val pgDriverVersion    = "42.2.5"
val notifyjsVersion    = "0.2.0"
val scriptsVersion     = "1.1.4"

// resolvers in ThisBuild += "hmil" at "https://dl.bintray.com/hmil/maven"
resolvers in ThisBuild += Resolver.bintrayRepo("hmil", "maven")

useJCenter := true

scalaVersion in ThisBuild := myScalaVersion

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
).enablePlugins(JSDependenciesPlugin, ScalaJSPlugin, ScalaJSWeb)
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
).enablePlugins(JSDependenciesPlugin, ScalaJSPlugin, ScalaJSWeb)
 .dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .jsConfigure(_.enablePlugins(JSDependenciesPlugin))
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
    ),
    jsDependencies += "org.webjars" % "jquery"   % "2.2.4" / "2.2.4/jquery.js",
    jsDependencies += "org.webjars" % "notifyjs" % "0.4.2" / "0.4.2/notify.js",
  )
  .enablePlugins(JSDependenciesPlugin, ScalaJSPlugin, ScalaJSWeb)

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
  version := "0.6.1"
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
