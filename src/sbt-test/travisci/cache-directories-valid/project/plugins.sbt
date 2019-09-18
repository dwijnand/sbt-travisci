sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.dwijnand" % "sbt-travisci" % x)
  case _       => sys.error("""|The system property 'plugin.version' is not defined.
                               |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.2")
