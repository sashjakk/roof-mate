version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.10"

val catsVersion = "2.9.0"
val circeVersion = "0.14.5"

lazy val root = (project in file("."))
  .settings(
    name := "roof-mate",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % "3.4.8",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % "test",
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0" % "test"
    )
  )
