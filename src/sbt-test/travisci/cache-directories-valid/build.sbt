def assertEq[A](left: A, right: A) = assert(left == right, s"$left != $right")

TaskKey[Unit]("check") := {
  assertEq(travisCacheConfiguredWarnings.value, Nil)
}
