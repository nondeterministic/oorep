import sbt.Keys.libraryDependencies
import sbtcrossproject.{CrossType, crossProject}

val scalaJSReactVersion = "1.2.3"
val scalaCssVersion = "0.5.5"
val reactJSVersion = "16.3.2"
// val circeVersion = "0.9.3"
val circeVersion = "0.10.0"
val scalaTestVersion = "3.1.0"

resolvers += Resolver.bintrayRepo("hmil", "maven")

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
    "org.postgresql" % "postgresql" % "42.2.5",
    "io.getquill" %% "quill-jdbc" % "3.2.2",
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
    "org.scala-js" %%% "scalajs-dom" % "0.9.5",
    "com.lihaoyi" %%% "scalatags" % "0.6.7",
    "org.querki" %%% "jquery-facade" % "1.2",
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "fr.hmil" %%% "roshttp" % "2.2.3", 
   "com.timushev" %%% "scalatags-rx" % "0.3.0"
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
    "org.scala-js" %%% "scalajs-dom" % "0.9.5",
    "com.lihaoyi" %%% "scalatags" % "0.6.7",
    "org.querki" %%% "jquery-facade" % "1.2",
    "io.circe" %%% "circe-core" % circeVersion,
    "io.circe" %%% "circe-generic" % circeVersion,
    "io.circe" %%% "circe-parser" % circeVersion,
    "fr.hmil" %%% "roshttp" % "2.2.3", 
   "com.timushev" %%% "scalatags-rx" % "0.3.0"
  ),
).enablePlugins(ScalaJSPlugin, ScalaJSWeb)
 .dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(
    dependencyOverrides += "org.webjars.npm" % "js-tokens" % "3.0.2",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "org.scala-js" %%% "scalajs-dom" % "0.9.5",
      "com.lihaoyi" %%% "scalatags" % "0.6.7",
      "org.querki" %%% "jquery-facade" % "1.2",
      "fr.hmil" %%% "roshttp" % "2.2.3",
      "com.timushev" %%% "scalatags-rx" % "0.3.0"
    )
  )
//  .jsSettings(
//    // skip in packageJSDependencies := false,
//    libraryDependencies ++= Seq(
//      "io.circe" %%% "circe-core" % "0.9.3",
//      "io.circe" %%% "circe-generic" % "0.9.3",
//      "io.circe" %%% "circe-parser" % "0.9.3",
//      "org.scala-js" %%% "scalajs-dom" % "0.9.5",
//      "com.lihaoyi" %%% "scalatags" % "0.6.7",
//      "org.querki" %%% "jquery-facade" % "1.2",
//      "fr.hmil" %%% "roshttp" % "2.2.3"
//    )
//  )
//  .jvmSettings(
//    libraryDependencies ++= Seq(
//      "io.circe" %% "circe-core" % "0.9.3",
//      "io.circe" %% "circe-generic" % "0.9.3",
//      "io.circe" %% "circe-parser" % "0.9.3"
//    )
//  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val commonSettings = Seq(
  scalaVersion := "2.12.5",
  organization := "org.multics.baueran.frep"
)

// loads the frontend project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project backend" :: s}
