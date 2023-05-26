// Comment to get more information during initialization
logLevel := Level.Warn

// Resolvers
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Sbt plugins
addSbtPlugin("com.vmunier"               % "sbt-web-scalajs"               % "1.0.10")  // Watch out to update this from 1.0.10 to 1.1 or even 1.2!
addSbtPlugin("org.scala-js"              % "sbt-scalajs"                   % "1.13.2")
addSbtPlugin("org.scala-js"              % "sbt-jsdependencies"            % "1.0.2")

addSbtPlugin("org.portable-scala"        % "sbt-scalajs-crossproject"      % "1.3.1")
addSbtPlugin("org.portable-scala"        % "sbt-scala-native-crossproject" % "1.3.1")
addSbtPlugin("org.scala-native"          % "sbt-scala-native"              % "0.4.14")

addSbtPlugin("com.typesafe.play"         % "sbt-plugin"                    % "2.8.20")
addSbtPlugin("com.typesafe.sbt"          % "sbt-gzip"                      % "1.0.2")
addSbtPlugin("com.typesafe.sbt"          % "sbt-digest"                    % "1.1.4")
