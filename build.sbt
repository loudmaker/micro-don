name := """micro-don"""

version := "1.0-SNAPSHOT"

inThisBuild(
  List(
    scalaVersion := "2.11.11"
  )
)


//lazy val GatlingTest = config("gatling") extend Test
lazy val root = (project in file(".")).enablePlugins(PlayJava)
/*
lazy val root = (project in file(".")).enablePlugins(PlayJava, GatlingPlugin).configs(GatlingTest)
  .settings(inConfig(GatlingTest)(Defaults.testSettings): _*)
  .settings(
    scalaSource in GatlingTest := baseDirectory.value / "/gatling/simulation"
  )
*/
//libraryDependencies += guice
//libraryDependencies += javaJpa
libraryDependencies += javaWs
/*
libraryDependencies += "com.h2database" % "h2" % "1.4.194"

libraryDependencies += "org.hibernate" % "hibernate-core" % "5.2.9.Final"
libraryDependencies += "io.dropwizard.metrics" % "metrics-core" % "3.2.1"
libraryDependencies += "com.palominolabs.http" % "url-builder" % "1.1.0"
libraryDependencies += "net.jodah" % "failsafe" % "1.0.3"

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.3.0" % Test
libraryDependencies += "io.gatling" % "gatling-test-framework" % "2.3.0" % Test
*/
PlayKeys.externalizeResources := false

//testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))
