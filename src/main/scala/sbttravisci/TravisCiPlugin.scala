package sbttravisci

import sbt._
import Keys._

import scala.util.Try

object TravisCiPlugin extends AutoPlugin {

  object autoImport {
    val isTravisBuild = settingKey[Boolean]("Flag indicating whether the current build is running under Travis")
    val travisPrNumber = settingKey[Option[Int]]("Number of the PR, if the build is a pull request build. Empty otherwise")
  }

  import autoImport._

  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  override def globalSettings = Seq(
    isTravisBuild := sys.env.get("TRAVIS").isDefined,
    travisPrNumber := Try {
      sys.env.get("TRAVIS_PULL_REQUEST") map (_.toInt)
    } getOrElse None
  )

  override def buildSettings = Seq(
    scalaVersion := {
      if (isTravisBuild.value)
        sys.env.get("TRAVIS_SCALA_VERSION").get
      else
        crossScalaVersions
	  .value
	  .filter(v => v.forall(c => c.isDigit || c == '.')) // loose attempt at keeping only stable versions
	  .lastOption
	  .getOrElse {
            crossScalaVersions.value.last   // sort .travis.yml versions in ascending order
        }
    },

    // parses Scala versions out of .travis.yml (doesn't support build matrices)
    crossScalaVersions := {
      val log = sLog.value

      val manifest = baseDirectory.value / ".travis.yml"
      val default = (crossScalaVersions in Global).value     // this avoids cyclic dependency issues

      if (manifest.exists()) {
        import scala.collection.JavaConverters._
        Using.fileInputStream(manifest) { fis =>
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

          if (versions.isEmpty) {
            log.warn("unable to parse Scala versions out of .travis.yml; contents may be ill-structured")
            log.warn(s"defaulting Scala version to ${default.last}")

            default
          } else {
            versions
          }
        }
      } else {
        log.warn(s"unable to locate .travis.yml file; defaulting Scala version to ${default.last}")

        default
      }
    }
  )
}
