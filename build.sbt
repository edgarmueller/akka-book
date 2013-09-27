name := "akka-book"

version := "0.1"
 
scalaVersion := "2.10.2"
 
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
 
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"   % "2.2-M3",
  "com.typesafe.akka" %% "akka-slf4j"   % "2.2-M3",
  "com.typesafe.akka" %% "akka-remote"  % "2.2-M3",
  "com.typesafe.akka" %% "akka-agent"   % "2.2-M3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2-M3" % "test"
)

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"