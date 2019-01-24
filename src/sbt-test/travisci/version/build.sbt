def assertEq[A](left: A, right: A) = assert(left == right, s"$left != $right")

TaskKey[Unit]("check") := {
  assertEq(crossScalaVersions.value, Seq("2.12.0"))
  assertEq(scala210.value, "no-2.10-version")
  assertEq(scala211.value, "no-2.11-version")
  assertEq(scala212.value, "2.12.0")
  assertEq(scala213.value, "no-2.13-version")
}
