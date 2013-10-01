sbtPlugin := true

name := "sbt-multi-publish"

organization := "com.gramercysoftware"

version := "2.0.0"

scalaVersion := "2.10.2"

sbtVersion := "0.13.0"

description := "sbt plugin to publish to multiple repositories"

scalacOptions := Seq( "-deprecation", "-unchecked" )

//libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

publishTo <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT"))
    Some(Resolver.file("Snapshots", file("../davidharcombe.github.io/snapshots/")))
  else
    Some(Resolver.file("Releases", file("../davidharcombe.github.io/releases/")))
}