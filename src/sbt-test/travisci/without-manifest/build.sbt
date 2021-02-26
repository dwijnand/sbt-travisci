def assertEq[A](left: A, right: A) = assert(left == right, s"$left != $right")

TaskKey[Unit]("check") := {
  if (sbtVersion.value.startsWith("0.13")) {
    assertEq(crossScalaVersions.value, Seq("2.13.5"))
    assertEq(scala210.value, "2.13.5")
    assertEq(scala212.value, "no-2.12-version")
  } else {
    assertEq(crossScalaVersions.value, Seq("2.12.6"))
    assertEq(scala210.value, "no-2.10-version")
    assertEq(scala212.value, "2.12.6")
  }
  assertEq(scala211.value, "no-2.11-version")
  assertEq(scala213.value, "no-2.13-version")
}
