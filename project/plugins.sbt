// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Sbt plugins
// cf. https://github.com/vmunier/sbt-web-scalajs
addSbtPlugin("com.vmunier"               % "sbt-web-scalajs"           % "1.0.8-0.6")
addSbtPlugin("org.scala-js"              % "sbt-scalajs"               % "0.6.25") 
addSbtPlugin("com.typesafe.play"         % "sbt-plugin"                % "2.6.18")
addSbtPlugin("com.typesafe.sbteclipse"   % "sbteclipse-plugin"         % "5.2.4")
addSbtPlugin("org.portable-scala"        % "sbt-scalajs-crossproject"  % "0.5.0")
addSbtPlugin("com.typesafe.sbt"          % "sbt-gzip"                  % "1.0.2")
addSbtPlugin("com.typesafe.sbt"          % "sbt-digest"                % "1.1.4")

// dependencyOverrides += "org.webjars.npm" % "js-tokens" % "3.0.2"

