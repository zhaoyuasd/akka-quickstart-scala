name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.19"

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",

"com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
"com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
"org.scalatest"     %% "scalatest"            % "3.0.1"         % Test
)




