# [sbt-travisci][] [![travis-badge][]][travis] [![gitter-badge][]][gitter]

[sbt-travisci]:         https://github.com/dwijnand/sbt-travisci
[travis]:          https://travis-ci.org/dwijnand/sbt-travisci
[travis-badge]:    https://travis-ci.org/dwijnand/sbt-travisci.svg?branch=master
[gitter]:              https://gitter.im/dwijnand/sbt-travisci
[gitter-badge]: https://badges.gitter.im/dwijnand/sbt-travisci.svg

`sbt-travisci` is an [sbt](http://www.scala-sbt.org/) plugin to integrate with Travis CI.

Original idea from [@djspiewak][] in his [djspiewak/base.g8][] template.

[@djspiewak]: https://github.com/djspiewak
[djspiewak/base.g8]: https://github.com/djspiewak/base.g8/blob/d75ba6e1628517124bd867d190373ee777814354/src/main/g8/build.sbt

## Setup

Add this to your sbt build plugins, in either `project/plugins.sbt` or `project/travisci.sbt`:

    addSbtPlugin("com.dwijnand" % "sbt-travisci" % "1.1.3")

Then make sure to **NOT set the `crossScalaVersions` setting**, otherwise you will override `sbt-travisci`.

Other than that, as `sbt-travisci` is an AutoPlugin that is all that is required.

## Detail

- `isTravisBuild in Global` will be automatically be set to `true` if the current build is running under Travis
    CI.
- `travisPrNumber in Global` will be automatically set to the number of the pull request, in case the build is a pull request build. 
    Otherwise, it will have a value of `None`.
- `crossScalaVersions in ThisBuild` will be automatically set to the scala versions in `.travis.yml`, falling
    back (with warnings) to the value of `crossScalaVersions in Global` if it can't be found or parsed properly.
- `scalaVersion in ThisBuild` will be automatically set to `TRAVIS_SCALA_VERSION` if `isTravisBuild` is true,
    otherwise to the `last` version in `crossScalaVersions`, and so by default, sbt will assume you want to
    develop under the *last* version listed in `.travis.yml`.
- `scala212 in ThisBuild` will be automatically set to the unique 2.12.x version in `.travis.yml`, or
    "no-2.12-version" if none set and "multiple-2.12-versions" (with a warning log) if multiple set.  Same for
    `scala210`, `scala211`, and `scala213`.

### Example

Given a `.travis.yml` of:

```yaml
scala:
  - 2.11.8
  - 2.12.0
```

`scalaVersion in ThisBuild` will be `"2.12.0"`.

## Sanity checking a scala version key

As a sanity check, you can stop the build from loading by running a check during sbt's `onLoad`.
For instance, to make sure that `scala212` is defines a scala version you can use:

```scala
Global / onLoad := (Global / onLoad).value.andThen { s =>
  val v = scala212.value
  if (!CrossVersion.isScalaApiCompatible(v))
    throw new MessageOnlyException(
      s"Key scala212 doesn't define a scala version. Check .travis.yml is setup right. Version: $v"
    )
  s
}
```

This will return an error message like the following:

```
[error] Key scala212 doesn't define a scala version. Check .travis.yml is setup right. Version: no-2.12-version
```

## Licence

Copyright 2016 Dale Wijnand

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
