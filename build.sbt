sbtPlugin := true

name := "sbt-cxf-wsdl2java"

organization := "com.ebiznext.sbt.plugins"

version := "0.1.6"

lazy val scala211  = "2.11.12"

crossScalaVersions := List(scala211)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (version.value.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/ebiznext/sbt-cxf-wsdl2java</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/mit-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:ebiznext/sbt-cxf-wsdl2java.git</url>
    <connection>scm:git:git@github.com:ebiznext/sbt-cxf-wsdl2java.git</connection>
  </scm>
  <developers>
    <developer>
      <id>smanciot</id>
      <name>Stéphane Manciot</name>
      <url>http://www.linkedin.com/in/smanciot</url>
    </developer>
  </developers>)
