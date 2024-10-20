lazy val akkaHttpVersion = "10.6.3"
lazy val akkaVersion    = "2.9.6"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.fresco",
      scalaVersion    := "3.3.3"
    )),
    name := "fresco-backend",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-pki"                 % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.5.9",
      "com.amazonaws"     % "aws-java-sdk-dynamodb"     % "1.12.765",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.2.18"        % Test,
      "org.scalatestplus" %% "mockito-5-10"             % "3.2.18.0"      % Test,
    )
  )

assembly / assemblyJarName := "backend-assembly.jar"
assembly / mainClass := Some("com.fresco.app.FrescoApp")
