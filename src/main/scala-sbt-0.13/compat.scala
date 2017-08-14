package sbt

package io {
  object `package` {
    type Using[Source, T] = sbt.Using[Source, T]
    val Using = sbt.Using
  }
}
