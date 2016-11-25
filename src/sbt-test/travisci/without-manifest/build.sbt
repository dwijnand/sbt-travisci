def check[A](inc: A, exp: A) = assert(inc == exp, s"Versions mismatch: Expected $exp, Incoming $inc")

TaskKey[Unit]("check") := check(crossScalaVersions.value, Seq("2.12.0"))
