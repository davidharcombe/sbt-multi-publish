package com.gramercysoftware.multipublish

import sbt._
import sbt.Keys._
import org.apache.ivy.core.module.descriptor.{Artifact ⇒ IvyArtifact}

object MultiPublish extends Plugin {
  lazy val multiPublish = TaskKey[Unit]("multi-publish", "Publish to list of repos")

  lazy val repositoryList = SettingKey[Seq[Option[Resolver]]]("repositoryList", "List of repositories to publish to.")

  lazy val multiPublishTask = {
    multiPublish <<= (streams, repositoryList, ivyModule, packagedArtifacts, checksums, ivyLoggingLevel, ivyConfiguration, publishTo, scalaVersion) map {
      (s, rl, im, a, c, l, ic, pt, cv) ⇒
        implicit val log: Logger = s.log

        rl foreach {
          repo =>
            s.log.info("Processing resolver %s".format(repo.get.name))

            im.withModule(log) {
              case (ivy, module, default) =>
                val resolver = ConvertResolver(repo.get)(ivy.getSettings, log)
                s.log.debug("... resolver => %s".format(resolver))
                val artifacts: Seq[(IvyArtifact, File)] = IvyActions.mapArtifacts(module, getSV(cv), a)
                checkFilesPresent(artifacts)
                try {
                  resolver.beginPublishTransaction(module.getModuleRevisionId(), true);
                  for ((artifact, file) <- artifacts) {
                    s.log.debug("  >>> artifact => %s".format(artifact))
                    s.log.debug("  >>> file => %s".format(file.toString))
                    resolver.publish(artifact, file, true)
                  }
                  resolver.commitPublishTransaction
                } catch {
                  case e =>
                    try {
                      resolver.abortPublishTransaction()
                    } finally {
                      throw e
                    }
                }
            }

        }
    } dependsOn (packageBin in Compile)
  }

  private[this] def checkFilesPresent(artifacts: Seq[(IvyArtifact, File)])(implicit log: Logger) {
    val missing = artifacts filter { case (a, file) => !file.exists }
    if(missing.nonEmpty)
      log.error("Missing files for publishing:\n\t" + missing.map(_._2.getAbsolutePath).mkString("\n\t"))
  }

  private def getSV(scalaVersion: String): Option[(String) ⇒ String] = {
    Some((scalaVersion) ⇒ scalaVersion)
  }

  override lazy val settings = Seq(
    multiPublishTask,
    repositoryList := Seq.empty
  )
}
