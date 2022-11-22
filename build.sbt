import sbtcrossproject.CrossType
import org.checkerframework.checker.units.qual.C
import sbt.{CrossVersion, ThisBuild}
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import scala.sys.process._

val scalaVersion3 = "3.2.1"

// CI convenience
addCommandAlias("lint", "fmtCheck;fixCheck;headerCheckAll")
addCommandAlias("build", "compile")

// dev convenience
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheckAll")
addCommandAlias("fixCheck", "scalafixAll --check")
addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
addCommandAlias("lintFix", "headerCreateAll;scalafixAll;fmt")
addCommandAlias("testJVM", "diesel/test")
addCommandAlias("testJS", "dieselJS/test")

lazy val commonSettings = Seq(
  organization  := "com.ibm.diesel",
  scalaVersion  := scalaVersion3,
  versionScheme := Some("semver-spec")
)

lazy val copyrightSettings = Seq(
  startYear        := Some(2021),
  organizationName := "The Diesel Authors",
  licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
)

lazy val root: Project = project
  .in(file("."))
  .aggregate(dieselJVM, dieselJS, dieselI18nPlugin)
  .settings(commonSettings)
  .settings(
    name := "diesel-i18n-root"
  )
  .settings(copyrightSettings)

lazy val diesel = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .withoutSuffixFor(JVMPlatform)
  .in(file("diesel-i18n"))
  .settings(commonSettings)
  .settings(
    name := "diesel-i18n"
  )
  .settings(copyrightSettings)
  .settings(
    scalacOptions ++= Seq(
      "-source:3.0",
      // "-source:future-migration",
      // "-rewrite",
      "-new-syntax",
      "-unchecked",
      "-deprecation",
      "-feature",
      "-Xfatal-warnings",
      "-language:existentials"
    ),
    libraryDependencies ++= Seq(
      "com.lihaoyi"   %%% "sourcecode" % "0.3.0",
      "org.scalameta" %%% "munit"      % "1.0.0-M7" % Test
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
    ThisBuild / semanticdbEnabled := true
  )

lazy val dieselJS  = diesel.js
lazy val dieselJVM = diesel.jvm

lazy val dieselI18nPlugin: Project = project.in(file("diesel-i18n-plugin"))
  .enablePlugins(SbtPlugin)
  .disablePlugins(ScalafixPlugin)
  .settings(commonSettings)
  .settings(
    name               := "diesel-i18n-plugin",
    scalaVersion       := "2.12.17",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog  := false,
    semanticdbEnabled  := false
  )
  .settings(copyrightSettings)
  .settings(
    (Compile / test) := scripted.toTask("").value
  )
