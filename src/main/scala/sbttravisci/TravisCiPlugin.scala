package sbttravisci

import scala.util.Try

import sbt._
import sbt.Keys._
import sbt.io.Using

object TravisCiPlugin extends AutoPlugin {

  object autoImport {
    val isTravisBuild = settingKey[Boolean]("Flag indicating whether the current build is running under Travis")
    val isTravisCacheConfigured = settingKey[Boolean]("Flag indicating whether cache directories are properly configured for sbt/coursier")
    val travisPrNumber = settingKey[Option[Int]]("Number of the PR, if the build is a pull request build. Empty otherwise")

    val scala210 = settingKey[String]("The Scala 2.10 version")
    val scala211 = settingKey[String]("The Scala 2.11 version")
    val scala212 = settingKey[String]("The Scala 2.12 version")
    val scala213 = settingKey[String]("The Scala 2.13 version")
  }

  import autoImport._

  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements

  override def globalSettings = Seq(
    isTravisBuild := sys.env.get("TRAVIS").isDefined,
    travisPrNumber := Try(sys.env.get("TRAVIS_PULL_REQUEST").map(_.toInt)).getOrElse(None)
  )

  override def buildSettings = Seq(
    scala210 := scalaVersionFor("2.10").value,
    scala211 := scalaVersionFor("2.11").value,
    scala212 := scalaVersionFor("2.12").value,
    scala213 := scalaVersionFor("2.13").value,

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
          val yaml = Option(new org.yaml.snakeyaml.Yaml().load[Any](fis))
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
    },

    sbtVersion := {
      sys.env.getOrElse("TRAVIS_SBT_VERSION", crossSbtVersions.value.last)
    },

    // parses SBT versions out of .travis.yml
    crossSbtVersions := {
      val log = sLog.value

      val manifest = baseDirectory.value / ".travis.yml"
      val default = (crossSbtVersions in Global).?.value.getOrElse(Seq.empty) match {
        case Seq() => Seq((sbtVersion in Global).value)
        case otherwise => otherwise
      }

      def parseEnvLine(str: String): Option[String] = {
        "TRAVIS_SBT_VERSION=\"?([a-zA-Z0-9.-]+)".r.findFirstMatchIn(str).map(_.group(1))
      }

      if (manifest.exists()) {
        Using.fileInputStream(manifest) { fis =>
          val yaml = Option(new org.yaml.snakeyaml.Yaml().load[Any](fis))
            .collect { case map: java.util.Map[_, _] => map }

          import scala.collection.JavaConverters._
          val fromRoot = yaml
            .flatMap(map => Option(map get "env"))
            .collect {
              case envs: java.util.List[_] => envs.iterator.asScala.map(_.toString).map(parseEnvLine).toList.flatten
              case env: String             => parseEnvLine(env).toList
            }
            .getOrElse(Nil)

          val fromMatrixInclude: List[String] = yaml
            .flatMap(map => Option(map get "matrix"))
            .collect { case map: java.util.Map[_, _] => Option(map get "include") }
            .flatten
            .collect { case versions: java.util.List[_] =>
              versions.iterator.asScala
                .collect { case map: java.util.Map[_, _] => Option(map get "env").map(_.toString).flatMap(parseEnvLine) }
                .flatten
                .toList
            }
            .getOrElse(Nil)

          val versions = (fromRoot ++ fromMatrixInclude).distinct

          if (versions.isEmpty) {
            log.warn("unable to parse TRAVIS_SBT_VERSION out of .travis.yml; contents may be ill-structured")
            log.warn(s"defaulting SBT version to ${default.last}")
            default
          } else {
            versions
          }
        }
      } else {
        log.warn(s"unable to locate .travis.yml file; defaulting SBT version to ${default.last}")
        default
      }
    },

    isTravisCacheConfigured := {
      val manifest = baseDirectory.value / ".travis.yml"
      if (!manifest.exists()) {
        // warned in crossScalaVersions
        false
      } else {
        Using.fileInputStream(manifest) { fis =>
          val log = sLog.value
          val yaml = Option(new org.yaml.snakeyaml.Yaml().load[Any](fis))
            .collect { case map: java.util.Map[_, _] => map }
          import scala.collection.JavaConverters._
          val cacheDirectories = yaml
            .flatMap(map => Option(map.get("cache")))
            .collect { case map: java.util.Map[_, _] => Option(map.get("directories")) }
            .flatten
            .collect { case directories: java.util.List[_] =>
              directories.iterator.asScala
                .collect { case dir: String => dir }
                .toList
            }
            .getOrElse(Nil)

          val isCoursierUsed = sbtBinaryVersion.value >= "1.3"
          val (sbtDir, ivy2CacheDir, coursierCacheDir) = sys.env.get("TRAVIS_OS_NAME") match {
            case Some("windows") => ("\\.sbt", "\\.ivy2\\cache","\\Coursier\\Cache")
            case Some("osx") => ("/.sbt", "/.ivy2/cache","/Library/Caches/Coursier")
            case _ => ("/.sbt", "/.ivy2/cache", "/.cache/coursier")
          }
          Seq(
            if (cacheDirectories.exists(_.endsWith(sbtDir))) {
              true
            } else {
              log.warn(s"${sbtDir} is not found in cache.directories in .travis.yml")
              false
            },
            if (!cacheDirectories.exists(_.endsWith(ivy2CacheDir))) {
              log.warn(s"${ivy2CacheDir} is not found in cache.directories in .travis.yml")
              false
            } else {
              true
            },
            if (!isCoursierUsed || cacheDirectories.exists(_.endsWith(coursierCacheDir))) {
              true
            } else {
              log.warn(s"${coursierCacheDir} is not found in cache.directories in .travis.yml")
              false
            }
          ).reduce(_ && _)
        }
      }
    }
  )

  private def scalaVersionFor(base: String) = Def.setting {
    val log = sLog.value
    val csv = (crossScalaVersions in ThisBuild).value
    val x = csv.filter(_.startsWith(s"$base.")) match {
      case Nil => s"no-$base-version"
      case Seq(version) => version
      case versions =>
        log.warn(s"Multiple Scala $base versions found in .travis.yml")
        s"multiple-$base-versions"
    }
    x
  }
}
