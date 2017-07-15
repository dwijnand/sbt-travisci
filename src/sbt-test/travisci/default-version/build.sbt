def check[A](inc: A, exp: A) = assert(inc == exp, s"Versions mismatch: Expected $exp, Incoming $inc")

TaskKey[Unit]("check") := {
  check(scalaVersion.value, "2.12.2")
  check(crossScalaVersions.value, Seq("2.12.2", "2.13.0-M1"))
}
