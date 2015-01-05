organization in ThisBuild := "me.lessis"

version in ThisBuild := "0.1.0-SNAPSHOT"

libraryDependencies in ThisBuild +=
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"

publishArtifact := false

publish := {}

lazy val seqd = project.in(file(".")).aggregate(`seqd-core`, `seqd-netty`)

lazy val `seqd-core` = project 

lazy val `seqd-netty` = project.dependsOn(`seqd-core`)
