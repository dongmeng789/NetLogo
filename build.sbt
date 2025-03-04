import sbtcrossproject.CrossPlugin.autoImport.{ crossProject, CrossType }
import sbtcrossproject.Platform

import ModelsLibrary.modelsDirectory
import Extensions.{ excludedExtensions, extensionRoot }
import NetLogoBuild.{ all, autogenRoot, cclArtifacts, includeInPackaging,
  marketingVersion, numericMarketingVersion, netlogoVersion, shareSourceDirectory }
import Docs.htmlDocs
import Dump.dumpClassName
import Testing.{ testTempDirectory, testChecksumsClass }

// these settings are common to ALL BUILDS
// if it doesn't belong in every build, it can't go in here
lazy val commonSettings = Seq(
  organization          := "org.nlogo",
  licenses              += ("GPL-2.0", url("http://opensource.org/licenses/GPL-2.0")),
  javaSource in Compile := baseDirectory.value / "src" / "main",
  javaSource in Test    := baseDirectory.value / "src" / "test",
  onLoadMessage         := "",
  testTempDirectory     := (baseDirectory.value.getParentFile / "tmp").getAbsoluteFile,
  ivyLoggingLevel       := UpdateLogging.Quiet,
  scalacOptions in Compile in console := scalacOptions.value.filterNot(_ == "-Xlint")
)

// These settings are common to all builds involving scala
// Any scala-specific settings should change here (and thus for all projects at once)
lazy val scalaSettings = Seq(
  scalaVersion           := "2.12.15",
  scalaSource in Compile := baseDirectory.value / "src" / "main",
  scalaSource in Test    := baseDirectory.value / "src" / "test",
  crossPaths             := false, // don't cross-build for different Scala versions
  scalacOptions ++=
    "-deprecation -unchecked -feature -Xcheckinit -encoding us-ascii -target:jvm-1.8 -opt:l:method -Xlint -Xfatal-warnings"
      .split(" ").toSeq,
  // we set doc options until https://github.com/scala/bug/issues/10402 is fixed
  scalacOptions in Compile in doc --= "-Xlint -Xfatal-warnings".split(" ").toSeq
)

// These settings are common to all builds that compile against Java
// Any java-specific settings should change here (and thus for all java projects at once)
lazy val jvmSettings = Seq(
  javaSource in Compile   := baseDirectory.value / "src" / "main",
  javaSource in Test      := baseDirectory.value / "src" / "test",
  publishArtifact in Test := true,
  javacOptions ++=
    "-g -deprecation -encoding us-ascii -Werror -Xlint:all -Xlint:-serial -Xlint:-fallthrough -Xlint:-path -source 1.8 -target 1.8"
    .split(" ").toSeq
)

// These are scalatest-specific settings
// Any scalatest-specific settings should change here
lazy val scalatestSettings = Seq(
  // show test failures again at end, after all tests complete.
  // T gives truncated stack traces; change to G if you need full.
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oS"),
  logBuffered in testOnly in Test := false,
  libraryDependencies ++= Seq(
    "org.scalatest"     %% "scalatest"       % "3.2.10"   % Test,
    "org.scalatestplus" %% "scalacheck-1-15" % "3.2.10.0" % Test,
  )
)

lazy val flexmarkDependencies = {
  val flexmarkVersion = "0.20.0"
  Seq(
    libraryDependencies ++= Seq(
      "com.vladsch.flexmark" % "flexmark" % flexmarkVersion,
      "com.vladsch.flexmark" % "flexmark-ext-autolink" % flexmarkVersion,
      "com.vladsch.flexmark" % "flexmark-ext-escaped-character" % flexmarkVersion,
      "com.vladsch.flexmark" % "flexmark-ext-typographic" % flexmarkVersion,
      "com.vladsch.flexmark" % "flexmark-util" % flexmarkVersion
      )
    )
}

lazy val mockDependencies = Seq(
  libraryDependencies ++= Seq(
    "org.jmock" % "jmock" % "2.8.1" % "test",
    "org.jmock" % "jmock-legacy" % "2.8.1" % "test",
    "org.jmock" % "jmock-junit4" % "2.8.1" % "test",
    "org.reflections" % "reflections" % "0.9.10" % "test",
    "org.slf4j" % "slf4j-nop" % "1.7.32" % "test"
  )
)

lazy val scalastyleSettings = Seq(
  scalastyleTarget in Compile := {
    baseDirectory.value.getParentFile / "target" / s"scalastyle-result-${name.value}.xml"
  })

lazy val root =
   project.in(file(".")).
   aggregate(netlogo, parserJVM)

lazy val netlogo = project.in(file("netlogo-gui")).
  dependsOn(parserJVM % "test->test;compile->compile").
  settings(NetLogoBuild.settings: _*).
  settings(includeInPackaging(parserJVM): _*).
  settings(shareSourceDirectory("netlogo-core"): _*).
  settings(commonSettings: _*).
  settings(jvmSettings: _*).
  settings(scalaSettings: _*).
  settings(scalatestSettings: _*).
  settings(JFlexRunner.settings: _*).
  settings(EventsGenerator.settings: _*).
  settings(Docs.settings: _*).
  settings(flexmarkDependencies).
  settings(Defaults.coreDefaultSettings ++
           Testing.settings ++
           Testing.useLanguageTestPrefix("org.nlogo.headless.Test") ++
           Packaging.settings ++
           Running.settings ++
           Dump.settings ++
           Scaladoc.settings ++
           ChecksumsAndPreviews.settings ++
           Extensions.settings ++
           InfoTab.infoTabTask ++
           ModelsLibrary.settings ++
           NativeLibs.nativeLibsTask ++
           NetLogoWebExport.settings ++
           GUISettings.settings ++
           Depend.dependTask: _*).
  settings(
    name := "NetLogo",
    version := "6.2.2",
    isSnapshot := true,
    publishTo := { Some("Cloudsmith API" at "https://maven.cloudsmith.io/netlogo/netlogo/") },
    mainClass in Compile := Some("org.nlogo.app.App"),
    modelsDirectory := baseDirectory.value.getParentFile / "models",
    extensionRoot   := (baseDirectory.value.getParentFile / "extensions").getAbsoluteFile,
    autogenRoot     := baseDirectory.value.getParentFile / "autogen",
    unmanagedSourceDirectories in Test      += baseDirectory.value / "src" / "tools",
    testChecksumsClass in Test              := "org.nlogo.headless.TestChecksums",
    resourceDirectory in Compile            := baseDirectory.value / "resources",
    unmanagedResourceDirectories in Compile ++= (unmanagedResourceDirectories in Compile in sharedResources).value,
    resourceGenerators in Compile += I18n.resourceGeneratorTask.taskValue,
    threed := { System.setProperty("org.nlogo.is3d", "true") },
    nogen  := { System.setProperty("org.nlogo.noGenerator", "true") },
    noopt  := { System.setProperty("org.nlogo.noOptimizer", "true") },
    libraryDependencies ++= Seq(
      "org.ow2.asm" % "asm-all" % "5.2",
      "org.picocontainer" % "picocontainer" % "2.15",
      "javax.media" % "jmf" % "2.1.1e",
      "commons-codec" % "commons-codec" % "1.15",
      "org.parboiled" %% "parboiled" % "2.3.0",
      "org.jogamp.jogl" % "jogl-all" % "2.4.0" from "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/jogl-all.jar",
      "org.jogamp.gluegen" % "gluegen-rt" % "2.4.0" from "https://jogamp.org/deployment/archive/rc/v2.4.0-rc-20210111/jar/gluegen-rt.jar",
      "org.jhotdraw" % "jhotdraw" % "6.0b1" % "provided,optional" from cclArtifacts("jhotdraw-6.0b1.jar"),
      "org.jmock" % "jmock" % "2.5.1" % "test",
      "org.jmock" % "jmock-legacy" % "2.5.1" % "test",
      "org.jmock" % "jmock-junit4" % "2.5.1" % "test",
      "org.apache.httpcomponents" % "httpclient" % "4.2",
      "org.apache.httpcomponents" % "httpmime" % "4.2",
      "com.googlecode.json-simple" % "json-simple" % "1.1.1",
      "com.fifesoft" % "rsyntaxtextarea" % "3.1.3",
      "com.typesafe" % "config" % "1.4.1",
      "net.lingala.zip4j" % "zip4j" % "2.9.0"
    ),
    all := {},
    all := {
      all.dependsOn(
        htmlDocs,
        packageBin in Test,
        Extensions.extensions,
        NativeLibs.nativeLibs,
        ModelsLibrary.modelIndex,
        Scaladoc.apiScaladoc).value
    }
  )

lazy val threed = TaskKey[Unit]("threed", "enable NetLogo 3D")
lazy val nogen = TaskKey[Unit]("nogen", "disable bytecode generator")
lazy val noopt = TaskKey[Unit]("noopt", "disable compiler optimizations")

lazy val headless = (project in file ("netlogo-headless")).
  dependsOn(parserJVM % "test-internal->test;compile-internal->compile").
  enablePlugins(org.nlogo.build.PublishVersioned).
  settings(commonSettings: _*).
  settings(scalaSettings: _*).
  settings(scalastyleSettings: _*).
  settings(jvmSettings: _*).
  settings(scalatestSettings: _*).
  settings(mockDependencies: _*).
  settings(Scaladoc.settings: _*).
  settings(Testing.settings: _*).
  settings(Testing.useLanguageTestPrefix("org.nlogo.headless.lang.Test"): _*).
  settings(Depend.dependTask: _*).
  settings(Extensions.settings: _*).
  settings(JFlexRunner.settings: _*).
  settings(includeInPackaging(parserJVM): _*).
  settings(shareSourceDirectory("netlogo-core"): _*).
  settings(Dump.settings: _*).
  settings(ChecksumsAndPreviews.settings: _*).
  settings(
    name          := "NetLogoHeadless",
    version       := "6.2.2",
    isSnapshot    := true,
    publishTo     := { Some("Cloudsmith API" at "https://maven.cloudsmith.io/netlogo/netlogo/") },
    autogenRoot   := (baseDirectory.value.getParentFile / "autogen").getAbsoluteFile,
    extensionRoot := baseDirectory.value.getParentFile / "extensions",
    mainClass in Compile         := Some("org.nlogo.headless.Main"),
    nogen                        := { System.setProperty("org.nlogo.noGenerator", "true") },
    libraryDependencies          ++= Seq(
      "org.ow2.asm" % "asm-all" % "5.2",
      "org.parboiled" %% "parboiled" % "2.3.0",
      "commons-codec" % "commons-codec" % "1.15",
      "com.typesafe" % "config" % "1.4.1",
      "net.lingala.zip4j" % "zip4j" % "2.9.0"
    ),
    (fullClasspath in Runtime)   ++= (fullClasspath in Runtime in parserJVM).value,
    resourceDirectory in Compile := baseDirectory.value / "resources" / "main",
    resourceDirectory in Test    := baseDirectory.value.getParentFile / "test",
    testChecksumsClass in Test   := "org.nlogo.headless.misc.TestChecksums",
    dumpClassName                := "org.nlogo.headless.misc.Dump",
    excludedExtensions           := Seq("arduino", "bitmap", "csv", "gis", "gogo", "ls", "nw", "palette", "py", "sound", "vid", "view2.5d"),
    all := { val _ = (
      (packageBin in Compile).value,
      (packageBin in Test).value,
      (compile in Test).value,
      Extensions.extensions
    )}
  )

 // this project exists as a wrapper for the mac-specific NetLogo components
lazy val macApp = project.in(file("mac-app")).
  settings(commonSettings: _*).
  settings(jvmSettings: _*).
  settings(scalaSettings: _*).
  settings(JavaPackager.mainArtifactSettings: _*).
  settings(NativeLibs.cocoaLibsTask).
  settings(Running.settings).
  settings(
    mainClass in Compile in run           := Some("org.nlogo.app.MacApplication"),
    fork in run                           := true,
    name                                  := "NetLogo-Mac-App",
    compile in Compile                    := {
      ((compile in Compile) dependsOn (packageBin in Compile in netlogo)).value
    },
    unmanagedJars in Compile              += (packageBin in Compile in netlogo).value,
    libraryDependencies                   ++= Seq(
      "net.java.dev.jna" % "jna" % "4.2.2",
      "ca.weblite" % "java-objc-bridge" % "1.0.0"),
    libraryDependencies                   ++= (libraryDependencies in netlogo).value,
    libraryDependencies                   ++= (libraryDependencies in parserJVM).value,
    run in Compile                        := {
      ((run in Compile) dependsOn NativeLibs.cocoaLibs).evaluated
    },
    javaOptions in run                    += "-Djava.library.path=" + (Seq(
      baseDirectory.value / "natives" / "macosx-universal" / "libjcocoa.dylib") ++
      ((baseDirectory in netlogo).value / "natives" / "macosx-universal" * "*.jnilib").get).mkString(":"),
    artifactPath in Compile in packageBin := target.value / "netlogo-mac-app.jar",
    javacOptions                          ++= Seq("-bootclasspath", System.getProperty("java.home") + "/lib/rt.jar"))

// this project is all about packaging NetLogo for distribution
lazy val dist = project.in(file("dist")).
  settings(NetLogoBuild.settings: _*).
  settings(NetLogoPackaging.settings(netlogo, macApp, behaviorsearchProject): _*)

lazy val sharedResources = (project in file ("shared")).
  settings(commonSettings: _*).
  settings(scalaSettings: _*).
  settings(scalastyleSettings: _*).
  settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources" / "main")

lazy val macros = (project in file("macros")).
  dependsOn(sharedResources).
  settings(commonSettings: _*).
  settings(scalaSettings: _*).
  settings(scalastyleSettings: _*).
  settings(libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value)

lazy val parser = crossProject(JSPlatform, JVMPlatform).
  crossType(new CrossType {
    override def projectDir(crossBase: File, projectType: String): File =
      crossBase / s"parser-$projectType"
    override def projectDir(crossBase: File, platform: Platform): File =
      projectDir(crossBase, if (platform == JSPlatform) "js" else "jvm")
    override def sharedSrcDir(projectBase: File, conf: String): Option[File] =
      Some(projectBase / "parser-core" / "src" / conf)
  }).
  in(file(".")).
  settings(commonSettings: _*).
  settings(scalaSettings: _*).
  settings(scalastyleSettings: _*).
  settings(
    isSnapshot := true,
    name := "parser",
    publishTo := { Some("Cloudsmith API" at "https://maven.cloudsmith.io/netlogo/netlogo/") },
    version := "0.3.0",
    unmanagedSourceDirectories in Compile += baseDirectory.value.getParentFile / "parser-core" / "src" / "main",
    unmanagedSourceDirectories in Test    += baseDirectory.value.getParentFile / "parser-core" / "src" / "test").
  jsConfigure(_.dependsOn(sharedResources % "compile-internal->compile")).
  jsConfigure(_.dependsOn(         macros % "compile-internal->compile;test-internal->compile")).
  jsSettings(
    name := "parser-js",
    scalaModuleInfo := scalaModuleInfo.value map { _.withOverrideScalaVersion(true) },
    resolvers += Resolver.sonatypeRepo("releases"),
    parallelExecution in Test := false,
    libraryDependencies ++= {
    import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
      Seq(
        "org.scala-lang.modules" %%% "scala-parser-combinators" %    "2.1.0"
      ,          "org.scalatest"  %%                "scalatest" %   "3.2.10" % Test
      ,      "org.scalatestplus"  %%          "scalacheck-1-15" % "3.2.10.0" % Test
    )}).
  jvmConfigure(_.dependsOn(sharedResources % "compile-internal->compile")).
  jvmSettings(jvmSettings: _*).
  jvmSettings(scalatestSettings: _*).
  jvmSettings(
    libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0",
    // you can get these included by just depending on the `sharedResources` project directly
    // but then when you publish the parser JVM package, the POM file lists sharedResources
    // as a dependency.  It seems like a weird thing to publish separately, so this just
    // gets them jammed into the jar of the parser for use (as NetLogo does, too).
    // -Jeremy B May 2022
    Compile / unmanagedResourceDirectories ++= (sharedResources / Compile / unmanagedResourceDirectories).value,
    Compile / resourceGenerators           ++= (sharedResources / Compile / resourceGenerators).value
  )

lazy val parserJVM = parser.jvm
lazy val parserJS  = parser.js

// only exists for scalastyling
lazy val parserCore = (project in file("parser-core")).
  settings(scalastyleSettings: _*).
  settings(skip in (Compile, compile) := true)

// only exists for scalastyling
lazy val netlogoCore = (project in file("netlogo-core")).
  settings(scalastyleSettings: _*).
  settings(skip in (Compile, compile) := true)

// only exists for packaging
lazy val behaviorsearchProject: Project =
  project.in(file("behaviorsearch"))
    .dependsOn(netlogo % "test-internal->test;compile-internal->compile")
    .settings(description := "subproject of NetLogo")
