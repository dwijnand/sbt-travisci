val sbttravisci = project in file(".")

organization := "com.dwijnand"
        name := "sbt-travisci"
     version := "0.1.0-SNAPSHOT"

   sbtPlugin := true
scalaVersion := "2.10.6"

libraryDependencies += "org.yaml" % "snakeyaml" % "1.17"
