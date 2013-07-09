sbt-multi-publish
=================

A simple sbt plugin to allow publishing to many repositories with one command, and with no need to edit the build.sbt file each time.

# Setup

Ensure the following lines are included in the project/plugins.sbt:

	resolvers ++= Seq(
		"David's Snapshots" at "http://davidharcombe.github.io/snapshots",
		"David's Releases" at "http://davidharcombe.github.io/releases"
	)

	addSbtPlugin("com.gramercysoftware" % "sbt-multi-publish0" % "<version>")
	
The current version is **1.0.0-SNAPSHOT**

## Usage ##

You can issue the command manually, as

	sbt multi-publish
	
or you can override the standard sbt `publish` command by adding the following line to your `build.sbt` file:

	publish <<= com.gramercysoftware.multipublish.MultiPublish.multiPublish

## Configuration ##

```
repositoryList <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT"))
    Seq(
        Some(Resolver.file("Github snapshots", file("../my.github.io/snapshots/"))),
        Some("Personal snapshots" at "http://my.repo.com/snapshots/"),
        Some("Remote snapshots" at "http://artifactory.organization.com/artifactory/snapshots")
    )
  else
    Seq(
        Some(Resolver.file("Github releases", file("../my.github.io/releases/"))),
        Some("Remote releases" at "http://artifactory.organization.com/artifactory/releases")
    )
}
```

The `repositoryList` is a `Seq[Option[Resolver]]`, each defined as would be any other sbt resolver. The same manner of detecting snapshots can be used as one would normally with the `publishTo` key.

When the `multi-publish` task runs, the final artifacts are published to each of the resolvers in turn.

## License

This project is released under the Apache License v2, for more details see the 'LICENSE' file.

## Contributing

Fork the project, add tests if possible and send a pull request.

## Contributors

David Harcombe

## Thanks

To Mark Harrah for sbt in the first place.