def check[A](inc: A, exp: A) = assert(inc == exp, s"Versions mismatch: Expected $exp, Incoming $inc")

TaskKey[Unit]("check") := {
  if (sbtVersion.value.startsWith("0.13"))
    check(crossScalaVersions.value, Seq("2.10.7"))
  else
    check(crossScalaVersions.value, Seq("2.12.6"))
}
