/*
 * sbt -- Simple Build Tool
 * Copyright 2008, 2009, 2010 Mark Harrah
 */
package com.gramercysoftware.multipublish

import java.util.Collections
import org.apache.ivy.core.LogOptions
import org.apache.ivy.util.Message
import org.apache.ivy.{core, plugins}
import core.module.id.ModuleRevisionId
import core.module.descriptor.DependencyDescriptor
import core.resolve.ResolveData
import core.settings.IvySettings
import org.apache.ivy.plugins.resolver._
import sbt._
import org.apache.ivy.core.module.descriptor.{Artifact ⇒ IvyArtifact}

/*
 * ConvertResolver has been taken from the IvySbt object in Ivy.scala. It is necessary in order to convert
 * an sbt Resolver to an Ivy DependencyResolver for publishing, but in sbt this object is private in scope,
 * making it unavailable so the code has had to be duplicated here, along with the fragment that deals
 * with ChainedResolvers.
 */
object ConvertResolver {
  /** Converts the given sbt resolver into an Ivy resolver.. */
  def apply(r: Resolver)(implicit settings: IvySettings, log: Logger) = {
    r match {
      case repo: MavenRepository => {
        val pattern = Collections.singletonList(resolvePattern(repo.root, Resolver.mavenStyleBasePattern))
        final class PluginCapableResolver extends IBiblioResolver with DescriptorRequired {
          def setPatterns() {
            // done this way for access to protected methods.
            setArtifactPatterns(pattern)
            setIvyPatterns(pattern)
          }
        }
        val resolver = new PluginCapableResolver
        initializeMavenStyle(resolver, repo.name, repo.root)
        resolver.setPatterns() // has to be done after initializeMavenStyle, which calls methods that overwrite the patterns
        resolver.setSettings(settings)
        resolver
      }
      case r: JavaNet1Repository => {
        // Thanks to Matthias Pfau for posting how to use the Maven 1 repository on java.net with Ivy:
        // http://www.nabble.com/Using-gradle-Ivy-with-special-maven-repositories-td23775489.html
        val resolver = new IBiblioResolver with DescriptorRequired {
          override def convertM2IdForResourceSearch(mrid: ModuleRevisionId) = mrid
        }
        initializeMavenStyle(resolver, JavaNet1Repository.name, "http://download.java.net/maven/1/")
        resolver.setPattern("[organisation]/[ext]s/[module]-[revision](-[classifier]).[ext]")
        resolver.setSettings(settings)
        resolver
      }
      case repo: SshRepository => {
        val resolver = new SshResolver with DescriptorRequired
        initializeSSHResolver(resolver, repo)
        repo.publishPermissions.foreach(perm => resolver.setPublishPermissions(perm))
        resolver.setSettings(settings)
        resolver
      }
      case repo: SftpRepository => {
        val resolver = new SFTPResolver
        initializeSSHResolver(resolver, repo)
        resolver.setSettings(settings)
        resolver
      }
      case repo: FileRepository => {
        val resolver = new FileSystemResolver with DescriptorRequired
        resolver.setName(repo.name)
        initializePatterns(resolver, repo.patterns)
        import repo.configuration.{isLocal, isTransactional}
        resolver.setLocal(isLocal)
        isTransactional.foreach(value => resolver.setTransactional(value.toString))
        resolver.setSettings(settings)
        resolver
      }
      case repo: URLRepository => {
        val resolver = new URLResolver with DescriptorRequired
        resolver.setName(repo.name)
        initializePatterns(resolver, repo.patterns)
        resolver.setSettings(settings)
        resolver
      }
      case repo: ChainedResolver => resolverChain(repo.name, repo.resolvers, false, settings, log)
      case repo: RawRepository =>
        val resolver = repo.resolver
        resolver.setSettings(settings)
        resolver
    }
  }

  private def resolverChain(name: String, resolvers: Seq[Resolver], localOnly: Boolean, settings: IvySettings, log: Logger): DependencyResolver = {
    val newDefault = new ChainResolver {
      def hasImplicitClassifier(artifact: IvyArtifact): Boolean = {
        import collection.JavaConversions._
        artifact.getQualifiedExtraAttributes.keys.exists(_.asInstanceOf[String] startsWith "m:")
      }

      // Technically, this should be applied to module configurations.
      // That would require custom subclasses of all resolver types in ConvertResolver (a delegation approach does not work).
      // It would be better to get proper support into Ivy.
      override def locate(artifact: IvyArtifact) =
        if (hasImplicitClassifier(artifact)) null else super.locate(artifact)

      override def getDependency(dd: DependencyDescriptor, data: ResolveData) = {
        if (data.getOptions.getLog != LogOptions.LOG_QUIET)
          Message.info("Resolving " + dd.getDependencyRevisionId + " ...")
        super.getDependency(dd, data)
      }
    }

    newDefault.setName(name)
    newDefault.setReturnFirst(true)
    newDefault.setCheckmodified(false)
    for (sbtResolver <- resolvers) {
      log.debug("\t" + sbtResolver)
      newDefault.add(ConvertResolver(sbtResolver)(settings, log))
    }

    newDefault.setSettings(settings)
    newDefault
  }

  private sealed trait DescriptorRequired extends BasicResolver {
    override def getDependency(dd: DependencyDescriptor, data: ResolveData) = {
      val prev = descriptorString(isAllownomd)
      setDescriptor(descriptorString(hasExplicitURL(dd)))
      try super.getDependency(dd, data) finally setDescriptor(prev)
    }

    def descriptorString(optional: Boolean) =
      if (optional) BasicResolver.DESCRIPTOR_OPTIONAL else BasicResolver.DESCRIPTOR_REQUIRED

    def hasExplicitURL(dd: DependencyDescriptor): Boolean =
      dd.getAllDependencyArtifacts.exists(_.getUrl != null)
  }

  private def initializeMavenStyle(resolver: IBiblioResolver, name: String, root: String) {
    resolver.setName(name)
    resolver.setM2compatible(true)
    resolver.setRoot(root)
  }

  private def initializeSSHResolver(resolver: AbstractSshBasedResolver, repo: SshBasedRepository)(implicit settings: IvySettings) {
    resolver.setName(repo.name)
    resolver.setPassfile(null)
    initializePatterns(resolver, repo.patterns)
    initializeConnection(resolver, repo.connection)
  }

  private def initializeConnection(resolver: AbstractSshBasedResolver, connection: RepositoryHelpers.SshConnection) {
    import resolver._
    import connection._
    hostname.foreach(setHost)
    port.foreach(setPort)
    authentication foreach {
      case RepositoryHelpers.PasswordAuthentication(user, password) =>
        setUser(user)
        password.foreach(setUserPassword)
      case RepositoryHelpers.KeyFileAuthentication(user, file, password) =>
        setKeyFile(file)
        password.foreach(setKeyFilePassword)
        setUser(user)
    }
  }

  private def initializePatterns(resolver: AbstractPatternsBasedResolver, patterns: Patterns)(implicit settings: IvySettings) {
    resolver.setM2compatible(patterns.isMavenCompatible)
    patterns.ivyPatterns.foreach(p => resolver.addIvyPattern(settings substitute p))
    patterns.artifactPatterns.foreach(p => resolver.addArtifactPattern(settings substitute p))
  }

  def resolvePattern(base: String, pattern: String): String = {
    val normBase = base.replace('\\', '/')
    if (normBase.endsWith("/") || pattern.startsWith("/")) normBase + pattern else normBase + "/" + pattern
  }
}

