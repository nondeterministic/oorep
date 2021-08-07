// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.bintrayRepo("hmil", "maven")

// Sbt plugins
// cf. https://github.com/vmunier/sbt-web-scalajs
addSbtPlugin("com.vmunier"               % "sbt-web-scalajs"               % "1.0.10")
addSbtPlugin("org.scala-js"              % "sbt-scalajs"                   % "1.0.0")
addSbtPlugin("org.scala-js"              % "sbt-jsdependencies"            % "1.0.0")

addSbtPlugin("org.portable-scala"        % "sbt-scalajs-crossproject"      % "1.0.0")
addSbtPlugin("org.portable-scala"        % "sbt-scala-native-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js"              % "sbt-scalajs"                   % "1.1.1")
addSbtPlugin("org.scala-native"          % "sbt-scala-native"              % "0.3.7")

addSbtPlugin("com.typesafe.play"         % "sbt-plugin"                    % "2.8.13")
addSbtPlugin("com.typesafe.sbt"          % "sbt-gzip"                      % "1.0.2")
addSbtPlugin("com.typesafe.sbt"          % "sbt-digest"                    % "1.1.4")
