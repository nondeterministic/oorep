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

resolvers in ThisBuild += Resolver.bintrayRepo("hmil", "maven")

javaOptions in Universal ++= Seq(
  "-Dpidfile.path=/dev/null"
)

lazy val backend = (project in file("backend")).settings(commonSettings).settings(
  scalaJSProjects := Seq(frontend, sec_frontend),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
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
  // cf. https://stackoverflow.com/questions/51481152/unresolved-webjars-dependency-even-though-it-seems-to-be-in-maven-central
  // dependencyOverrides += "org.webjars.npm" % "js-tokens" % "3.0.2",
  
  skip in packageJSDependencies := false,
 
  // https://stackoverflow.com/questions/37127313/scalajs-to-simply-redirect-complication-output-to-specified-directory
  artifactPath in(Compile, fastOptJS) :=
    baseDirectory.value / ".." / "backend" / "public" / "javascripts" / "frontend-pub-fastOpt.js",
 
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
  // cf. https://stackoverflow.com/questions/51481152/unresolved-webjars-dependency-even-though-it-seems-to-be-in-maven-central
  // dependencyOverrides += "org.webjars.npm" % "js-tokens" % "3.0.2",
  
  skip in packageJSDependencies := false,
 
  artifactPath in(Compile, fastOptJS) :=
    baseDirectory.value / ".." / "backend" / "public" / "javascripts" / "frontend-priv-fastOpt.js",

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
    // dependencyOverrides += "org.webjars.npm" % "js-tokens" % "3.0.2",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scala-js" %%% "scalajs-dom" % scalaJsDomVersion,
      "com.lihaoyi" %%% "scalatags" % scalaTagsVersion,
      "org.querki" %%% "jquery-facade" % scalaJQueryVersion,
      "fr.hmil" %%% "roshttp" % rosHttpVersion,
      "com.timushev" %%% "scalatags-rx" % scalatagsrxVersion
    )
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val commonSettings = Seq(
  scalaVersion := myScalaVersion,
  organization := "org.multics.baueran.frep"
)

// loads the frontend project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project backend" :: s}
