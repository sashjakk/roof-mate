version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.10"

val catsVersion = "2.9.0"
val circeVersion = "0.14.5"
val http4sVersion = "0.23.18"
val doobieVersion = "1.0.0-RC1"
val pureConfigVersion = "0.17.4"

lazy val root = (project in file("."))
  .settings(
    name := "roof-mate",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
      "org.typelevel" %% "cats-effect" % "3.4.8",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "org.http4s" %% "http4s-ember-client" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
      "com.github.geirolz" %% "fly4s-core" % "0.0.16",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % "test",
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0" % "test"
    ),
    assembly / mainClass := Some("com.github.sashjakk.http.Server"),
    assembly / assemblyJarName := "roof-mate.jar"
  )
