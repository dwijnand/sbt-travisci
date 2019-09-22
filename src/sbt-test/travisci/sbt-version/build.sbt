def assertEq[A](left: A, right: A) = assert(left == right, s"$left != $right")

TaskKey[Unit]("check") := {
  assertEq(sbtVersion.value, "0.13.0")
  assertEq(crossSbtVersions.value, Seq("0.13.0"))
}
