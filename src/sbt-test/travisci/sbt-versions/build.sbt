def assertEq[A](left: A, right: A) = assert(left == right, s"$left != $right")

TaskKey[Unit]("check") := {
  assertEq(sbtVersion.value, "1.2.0")
  assertEq((crossSbtVersions in ThisBuild).value, Seq("0.13.17", "1.2.0"))
  // This fails since crossSbtVerions.value is Vector("1.2.0") 
  // assertEq((crossSbtVersions).value, Seq("0.13.17", "1.2.0"))
}
