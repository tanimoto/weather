name := "weather"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  // Akka
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.2.1",
  // Slick
  "com.typesafe.play" %% "play-slick" % "0.5.0.2-SNAPSHOT",
  "com.github.tototoshi" %% "slick-joda-mapper" % "0.4.0",
  // Utils
  "com.github.nscala-time" %% "nscala-time" % "0.6.0",
  "commons-net" % "commons-net" % "3.3",
  "com.github.tototoshi" % "scala-csv_2.10" % "0.8.0",
  "com.jsuereth" %% "scala-arm" % "1.3",
  "com.github.scala-incubator.io" % "scala-io-core_2.10" % "0.4.2",
  // Testing
  "com.typesafe.akka" %% "akka-testkit" % "2.2.1" % "test"
)

play.Project.playScalaSettings
