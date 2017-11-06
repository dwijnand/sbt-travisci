          addSbtPlugin("com.dwijnand"       % "sbt-dynver"      % "2.0.0")
          addSbtPlugin("io.get-coursier"    % "sbt-coursier"    % "1.0.0-RC10")
libraryDependencies += "org.scala-sbt"     %% "scripted-plugin" % sbtVersion.value
          addSbtPlugin("com.typesafe"       % "sbt-mima-plugin" % "0.1.17")
          addSbtPlugin("org.foundweekends"  % "sbt-bintray"     % "0.5.1")
          addSbtPlugin("ch.epfl.scala"      % "sbt-scalafix"    % "0.5.3")
