import sbtcrossproject.CrossType
import org.checkerframework.checker.units.qual.C
import sbt.{CrossVersion, ThisBuild}
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import scala.sys.process._

// val scalaVersion3 = "3.2.1"
val scalaVersion2 = "2.13.14"

// CI convenience
addCommandAlias("lint", "fmtCheck;fixCheck;headerCheckAll")
addCommandAlias("build", "compile")

// dev convenience
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("fixCheck", "scalafixAll --check")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("lintFix", "headerCreateAll;scalafixAll;fmt")
addCommandAlias("testJVM", "diesel/test")
addCommandAlias("testJS", "diesel.js/test")

lazy val commonSettings = Seq(
  organization  := "com.ibm.cloud.diesel",
  scalaVersion  := scalaVersion2,
  versionScheme := Some("semver-spec"),
  description   := "Utilities for localizing Diesel components."
)

lazy val copyrightSettings = Seq(
  startYear        := Some(2021),
  organizationName := "The Diesel Authors",
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
)

import xerial.sbt.Sonatype._
lazy val sonatypeSettings = Seq(
  sonatypeProfileName    := "com.ibm.cloud",
  sonatypeProjectHosting := Some(
    GitHubHosting("IBM", "diesel-i18n", "agilecoderfrank@gmail.com")
  ),
  sonatypeCredentialHost := "oss.sonatype.org",
  sonatypeRepository     := "https://oss.sonatype.org/service/local"
)

lazy val root: Project = project
  .in(file("."))
  .aggregate(diesel.jvm, diesel.js, dieselI18nPlugin)
  .settings(commonSettings)
  .settings(sonatypeSettings)
  .settings(copyrightSettings)
  .settings(
    name           := "diesel-i18n-root",
    publish / skip := true
  )

lazy val diesel = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("diesel-i18n"))
  .settings(commonSettings)
  .settings(sonatypeSettings)
  .settings(copyrightSettings)
  .settings(
    name := "diesel-i18n"
  )
  .settings(
    scalacOptions ++= Seq(
      // "-source:3.0",
      // "-source:future-migration",
      // "-rewrite",
      // "-new-syntax",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-language:existentials"
    ),
    libraryDependencies ++= Seq(
      "org.scala-lang"  % "scala-reflect" % scalaVersion2,
      "org.scalameta" %%% "munit"         % "1.0.1" % Test
    ),
    Test / fork        := false,
    Test / logBuffered := false,
    // see https://github.com/scalameta/munit/blob/main/junit-interface/src/main/java/munit/internal/junitinterface/JUnitRunner.java
    // Test / testOptions += Tests.Argument("+l", "--summary=1")
    Test / testOptions += Tests.Argument("--summary=1")
  )
  .jsSettings(
    Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )
  .settings(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )

lazy val dieselI18nPlugin: Project = project.in(file("diesel-i18n-plugin"))
  .enablePlugins(SbtPlugin)
  .disablePlugins(ScalafixPlugin)
  .settings(commonSettings)
  .settings(sonatypeSettings)
  .settings(copyrightSettings)
  .settings(
    name               := "diesel-i18n-plugin",
    scalaVersion       := "2.12.20",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog  := false,
    semanticdbEnabled  := false
  )
  .settings(
    (Compile / test) := scripted.toTask("").value
  )
