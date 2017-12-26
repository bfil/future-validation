lazy val project = Project("future-validation", file("."))
  .settings(settings, libraryDependencies ++= Seq(
    "org.scalaz" %% "scalaz-core" % "7.2.18",
    "org.specs2" %% "specs2-core" % "3.8.6" % "test",
    "org.specs2" %% "specs2-mock" % "3.8.6" % "test",
    "org.mockito" % "mockito-all" % "1.10.19" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
  ))

lazy val settings = Seq(
  scalaVersion := "2.12.4",
  crossScalaVersions := Seq("2.12.4", "2.11.12", "2.10.7"),
  organization := "io.bfil",
  organizationName := "Bruno Filippone",
  organizationHomepage := Some(url("http://bfil.io")),
  homepage := Some(url("https://github.com/bfil/future-validation")),
  licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
  developers := List(
    Developer("bfil", "Bruno Filippone", "bruno@bfil.io", url("http://bfil.io"))
  ),
  startYear := Some(2015),
  publishTo := Some("Bintray" at s"https://api.bintray.com/maven/bfil/maven/${name.value}"),
  credentials += Credentials(Path.userHome / ".ivy2" / ".bintray-credentials"),
  scmInfo := Some(ScmInfo(
    url(s"https://github.com/bfil/future-validation"),
    s"git@github.com:bfil/future-validation.git"
  ))
)
