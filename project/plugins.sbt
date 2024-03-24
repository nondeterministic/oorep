// Comment to get more information during initialization
logLevel := Level.Warn
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Sbt plugins
addSbtPlugin("com.vmunier"               % "sbt-web-scalajs"               % "1.3.0")
addSbtPlugin("org.scala-js"              % "sbt-scalajs"                   % "1.17.0")
addSbtPlugin("org.scala-js"              % "sbt-jsdependencies"            % "1.0.2")

addSbtPlugin("org.portable-scala"        % "sbt-scalajs-crossproject"      % "1.3.1")
addSbtPlugin("org.portable-scala"        % "sbt-scala-native-crossproject" % "1.3.1")
addSbtPlugin("org.scala-native"          % "sbt-scala-native"              % "0.4.14")

addSbtPlugin("org.playframework"         % "sbt-plugin"                    % "3.0.6")
addSbtPlugin("com.typesafe.sbt"          % "sbt-gzip"                      % "1.0.2")
addSbtPlugin("com.typesafe.sbt"          % "sbt-digest"                    % "1.1.4")
