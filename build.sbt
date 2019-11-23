name := "ATMKeeperBeagleBoneApp"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.23",
  "com.typesafe.akka" %% "akka-remote" % "2.5.23",
  "javax.usb" % "usb-api" % "1.0.2",
  "org.usb4java" % "usb4java-javax" % "1.2.0"
)
