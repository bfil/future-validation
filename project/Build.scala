import sbt._
import Keys._
import com.bfil.sbt._

object ProjectBuild extends BFilBuild with BFilPlugins {

  lazy val root = BFilProject("future-validation", file("."))
    .settings(libraryDependencies ++= Dependencies.all)
    
}

object Dependencies {
  
  def all = Seq(
    "org.scalaz" %% "scalaz-core" % "7.1.4",
    "org.specs2" %% "specs2-core" % "3.6.4" % "test",
    "org.specs2" %% "specs2-mock" % "3.6.4" % "test",
    "org.mockito" % "mockito-all" % "1.10.8" % "test",
    "org.hamcrest" % "hamcrest-all" % "1.3" % "test"
  )
  
}