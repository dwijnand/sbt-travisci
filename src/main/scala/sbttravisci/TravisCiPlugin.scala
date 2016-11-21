package sbttravisci

import sbt._, Keys._

object TravisCiPlugin extends AutoPlugin {
  private val DefaultScalaVersion = "2.10.6"    // for compatibility with stock sbt

  object autoImport {
    val isTravisBuild = settingKey[Boolean]("Flag indicating whether the current build is running under Travis")
  }

  import autoImport._

  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements
  override def buildSettings = Seq(
    isTravisBuild := sys.env.get("TRAVIS").isDefined,

    scalaVersion := {
      if (isTravisBuild.value)
        sys.env.get("TRAVIS_SCALA_VERSION").get
      else
        crossScalaVersions.value.last   // sort .travis.yml versions in ascending order
    },

    // parses Scala versions out of .travis.yml (doesn't support build matrices)
    crossScalaVersions := {
      import scala.collection.JavaConverters._
      Using.fileInputStream(baseDirectory.value / ".travis.yml") { fis =>
        val yaml = Option(new org.yaml.snakeyaml.Yaml().load(fis))
          .collect { case map: java.util.Map[_, _] => map }

        def fromRoot = yaml
          .flatMap(map => Option(map get "scala"))
          .collect {
            case versions: java.util.List[_] => versions.asScala.toList map (_.toString)
            case version: String             => version :: Nil
          }
          .getOrElse(Nil)

        def fromMatrixInclude = yaml
          .flatMap(map => Option(map get "matrix"))
          .collect { case map: java.util.Map[_, _] => Option(map get "include") }.flatten
          .collect { case versions: java.util.List[_] =>
            versions.asScala.toList.collect { case map: java.util.Map[_, _] =>
              Option(map get "scala") map (_.toString)
            }.flatten
          }
          .getOrElse(Nil)

        val versions = fromRoot ++ fromMatrixInclude
        if (versions.isEmpty) List(DefaultScalaVersion) else versions
      }
    }
  )
}
