package sbttravisci

import sbt._, Keys._

object TravisCiPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger  = allRequirements
  override def buildSettings = Seq(
    // parses Scala versions out of .travis.yml (doesn't support build matrices)
    crossScalaVersions := {
      import scala.collection.JavaConverters._
      Using.fileInputStream(baseDirectory.value / ".travis.yml")(fis =>
        Option(new org.yaml.snakeyaml.Yaml().load(fis))
          .collect { case map: java.util.Map[_, _] => Option(map get "scala") }
          .flatten
          .collect {
            case versions: java.util.List[_] => versions.asScala.toList map (_.toString)
            case version: String             => version :: Nil
          }
          .getOrElse(List(scalaVersion.value))
      )
    }
  )
}
