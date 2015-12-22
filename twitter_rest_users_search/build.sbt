import play.Project._

name := """play-twitter-rest-users-search"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.2", 
  "org.webjars" % "bootstrap" % "2.3.1",
  "com.googlecode.json-simple" % "json-simple" % "1.1",
  "org.apache.jena" % "apache-jena-libs" % "3.0.1"
  )

playJavaSettings
