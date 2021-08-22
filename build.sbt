import Dependencies._

ThisBuild / scalaVersion     := "2.11.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "deps test",
    libraryDependencies += "com.factual" % "neutronic-batch" % "2.17.0",
    credentials += {
      val envCreds = for {
        envUser <- sys.env.get("MVN_USERNAME")
        envPass <- sys.env.get("MVN_PASSWORD")
      } yield Credentials("Artifactory Realm", "foursquaredev.jfrog.io", envUser, envPass)
      envCreds.getOrElse(Credentials(Path.userHome / ".sbt" / ".credentials"))
    },
    resolvers := Seq(
      "Foursquare Artifactory" at "https://foursquaredev.jfrog.io/foursquaredev/fsnexus"
    )
  )
