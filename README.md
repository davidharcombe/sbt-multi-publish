sbt-multi-publish
=================

A simple sbt plugin to allow publishing to many repositories with one command, and with no need to edit the build.sbt file each time.

# Setup

Ensure the following lines are included in the project/plugins.sbt:

	resolvers ++= Seq(
		"David's Snapshots" at "http://davidharcombe.github.io/snapshots",
		"David's Releases" at "http://davidharcombe.github.io/releases"
	)

	addSbtPlugin("com.gramercysoftware" % "sbt-multi-publish" % "<version>")
	
The current version is **1.0.1**

## Usage ##

You can issue the command manually, as

	sbt multi-publish
	
or you can override the standard sbt `publish` command by adding the following line to your `build.sbt` file:

    import com.gramercysoftware.multipublish.MultiPublish._

	publish <<= multiPublish

## Configuration ##

```
repositoryList <<= version { (v: String) =>
  if (v.trim.endsWith("-SNAPSHOT"))
    Seq(
        Resolver.file("Github snapshots", file("../my.github.io/snapshots/")),
        "Personal snapshots" at "http://my.repo.com/snapshots/",
        "Remote snapshots" at "http://artifactory.organization.com/artifactory/snapshots"
    )
  else
    Seq(
        Resolver.file("Github releases", file("../my.github.io/releases/")),
        "Remote releases" at "http://artifactory.organization.com/artifactory/releases"
    )
}
```

The `repositoryList` is a `Seq[Resolver]`, each defined as would be any other sbt resolver. The same manner of detecting snapshots can be used as one would normally with the `publishTo` key.

When the `multi-publish` task runs, the final artifacts are published to each of the resolvers in turn.

If you are overriding `publish`	, there's no need for a `publishTo` block in the `build.sbt` file.

If there is a `publishTo` block, the resolver from there is added to the list of repositories automatically, and 

## License

This project is released under the Apache License v2, for more details see the 'LICENSE' file.

## Contributing

Fork the project, add tests if possible and send a pull request.

## Contributors

David Harcombe

## Thanks

To Mark Harrah for sbt in the first place.