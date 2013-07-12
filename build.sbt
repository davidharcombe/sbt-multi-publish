sbtPlugin := true

name := "sbt-multi-publish"

organization := "com.gramercysoftware"

version := "1.0.1"

scalaVersion := "2.9.2"

sbtVersion := "0.12.0"

description := "sbt plugin to publish to multiple repositories"

scalacOptions := Seq( "-deprecation", "-unchecked" )

publishTo <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT"))
    Some(Resolver.file("Snapshots", file("../davidharcombe.github.io/snapshots/")))
  else
    Some(Resolver.file("Releases", file("../davidharcombe.github.io/releases/")))
}