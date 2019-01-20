package sbttravisci

import scala.util.Try

import sbt._
import sbt.Keys._
import sbt.io.Using

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
    travisPrNumber := Try(sys.env.get("TRAVIS_PULL_REQUEST").map(_.toInt)).getOrElse(None)
  )

  override def buildSettings = Seq(
    scalaVersion := {
      if (isTravisBuild.value)
        sys.env("TRAVIS_SCALA_VERSION")
      else
        crossScalaVersions.value.last   // sort .travis.yml versions in ascending order
    },

    // parses Scala versions out of .travis.yml
    crossScalaVersions := {
      val log = sLog.value

      val manifest = baseDirectory.value / ".travis.yml"
      val default = (crossScalaVersions in Global).value     // this avoids cyclic dependency issues

      if (manifest.exists()) {
        Using.fileInputStream(manifest) { fis =>
          val yaml = Option(new org.yaml.snakeyaml.Yaml().load(fis))
            .collect { case map: java.util.Map[_, _] => map }

          import scala.collection.JavaConverters._
          val fromRoot = yaml
            .flatMap(map => Option(map get "scala"))
            .collect {
              case versions: java.util.List[_] => versions.iterator.asScala.map(_.toString).toList
              case version: String             => version :: Nil
            }
            .getOrElse(Nil)

          val fromMatrixInclude = yaml
            .flatMap(map => Option(map get "matrix"))
            .collect { case map: java.util.Map[_, _] => Option(map get "include") }
            .flatten
            .collect { case versions: java.util.List[_] =>
              versions.iterator.asScala
                .collect { case map: java.util.Map[_, _] => Option(map get "scala").map(_.toString) }
                .flatten
                .toList
            }
            .getOrElse(Nil)

          val versions = (fromRoot ++ fromMatrixInclude).distinct

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
