name := "akka-in-action-memo"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies ++= {
  val akkaVersion = "2.5.4"
  val akkaHttpVersion ="10.0.10"
  Seq(
    "com.typesafe.akka" %%  "akka-actor"              % akkaVersion,
    "com.typesafe.akka" %%  "akka-stream"             % akkaVersion,
    "com.typesafe.akka" %%  "akka-slf4j"              % akkaVersion,

    "com.typesafe.akka" %%  "akka-remote"             % akkaVersion,
    "com.typesafe.akka" %%  "akka-multi-node-testkit" % akkaVersion % "test",

    "com.typesafe.akka" %%  "akka-testkit"            % akkaVersion % "test",
    "org.scalatest"     %%  "scalatest"               % "3.0.0"     % "test",
    "com.typesafe.akka" %%  "akka-http-core"          % akkaHttpVersion,
    "com.typesafe.akka" %%  "akka-http"               % akkaHttpVersion,
    "com.typesafe.akka" %%  "akka-http-spray-json"    % akkaHttpVersion,
    "ch.qos.logback"    %   "logback-classic"         % "1.1.6"
  )
}
