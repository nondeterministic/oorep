// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.bintrayRepo("hmil", "maven")

// Sbt plugins
// cf. https://github.com/vmunier/sbt-web-scalajs
addSbtPlugin("com.vmunier"               % "sbt-web-scalajs"           % "1.0.10-0.6")
addSbtPlugin("org.scala-js"              % "sbt-scalajs"               % "0.6.31")
addSbtPlugin("com.typesafe.play"         % "sbt-plugin"                % "2.6.23")
addSbtPlugin("org.portable-scala"        % "sbt-scalajs-crossproject"  % "0.6.1")
addSbtPlugin("com.typesafe.sbt"          % "sbt-gzip"                  % "1.0.2")
addSbtPlugin("com.typesafe.sbt"          % "sbt-digest"                % "1.1.4")
