def assertEq[A](left: A, right: A) = assert(left == right, s"$left != $right")

TaskKey[Unit]("check") := {
  // /.cache/coursier is not required since sbt is not 1.3
  assertEq(travisCacheConfiguredWarnings.value, List("/.sbt"))
}
