name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.13.1"

lazy val akkaVersion = "2.6.6"
val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "2.0.1",

  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-optics" % "0.12.0",

  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.6.6" % Test
)
