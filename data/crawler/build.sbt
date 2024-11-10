val scala3Version = "3.5.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "fresco-api",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "com.typesafe.akka" %% "akka-actor-typed" % "2.8.7",
      "com.typesafe.akka" %% "akka-stream" % "2.8.7",
      "com.typesafe.akka" %% "akka-http" % "10.5.3",
      "io.spray" %%  "spray-json" % "1.3.6",
      "org.json4s" %% "json4s-native" % "4.0.7",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.5.3",
      "com.amazonaws" % "aws-java-sdk-dynamodb" % "1.12.765",
      "com.amazonaws" % "aws-java-sdk-s3" % "1.12.765",
    )
  )
