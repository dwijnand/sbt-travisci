def assertEq[A](left: A, right: A) = assert(left == right, s"$left != $right")

TaskKey[Unit]("check") := {
  assertEq(crossScalaVersions.value, Seq("2.10.6", "2.11.8", "2.12.0"))
  assertEq(scala210.value, "2.10.6")
  assertEq(scala211.value, "2.11.8")
  assertEq(scala212.value, "2.12.0")
  assertEq(scala213.value, "no-2.13-version")
  assertEq(sbtVersion.value, "1.2.0")
  assertEq((crossSbtVersions in ThisBuild).value, Seq("0.13.17", "1.2.0"))
}
