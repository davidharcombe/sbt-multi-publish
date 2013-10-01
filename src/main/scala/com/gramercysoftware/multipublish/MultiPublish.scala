package com.gramercysoftware.multipublish

import sbt._
import sbt.Keys._
import org.apache.ivy.core.module.descriptor.{ Artifact ⇒ IvyArtifact }

object MultiPublish extends Plugin {
  lazy val multiPublish = TaskKey[Unit]("multi-publish", "Publish to list of repos")

  lazy val repositoryList = SettingKey[Seq[Resolver]]("repositoryList", "List of repositories to publish to.")

  lazy val multiPublishTask = {
    multiPublish <<= (streams, repositoryList, ivyModule, packagedArtifacts, ivyConfiguration, scalaVersion, publishTo) map {
      (s, rl, im, a, ic, sv, pt) ⇒
        implicit val log: Logger = s.log

        im.withModule(log) {
          case (ivy, module, _) ⇒
            implicit val settings = ivy.getSettings

            val artifacts: Seq[(IvyArtifact, File)] = IvyActions.mapArtifacts(module, Some((sv) ⇒ sv), a)

            {
              pt match {
                case None ⇒ rl
                case Some(resolver) ⇒ rl :+ resolver
              }
            }.toList.distinct foreach {
              resolver ⇒
                s.log.info(s"Processing resolver ${resolver.name}")
                val ivyResolver = ConvertResolver(resolver)
                IvyActions.publish(module, artifacts, ivyResolver, true)
            }

        }
    } dependsOn (packageBin in Compile)
  }

  private[this] def checkFilesPresent(artifacts: Seq[(IvyArtifact, File)])(implicit log: Logger) {
    val missing = artifacts filter {
      case (a, file) ⇒ !file.exists
    }
    if (missing.nonEmpty)
      log.error(s"Missing files for publishing:\n\t${missing.map(_._2.getAbsolutePath).mkString("\n\t")}")
  }

  override lazy val settings = Seq(
    multiPublishTask,
    repositoryList := Seq.empty)
}
