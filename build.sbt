name := "ATMKeeperBeagleBoneApp"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.23",
  "com.typesafe.akka" %% "akka-remote" % "2.5.23",
  "org.usb4java" % "usb4java" % "1.2.0",
  "org.usb4java" % "usb4java-javax" % "1.2.0"
)
