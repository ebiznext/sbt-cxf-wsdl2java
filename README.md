sbt-cxf-wsdl2java
=================

an sbt plugin for generating java artifacts from WSDL using cxf

## Requirements

* [SBT 0.13+](http://www.scala-sbt.org/)


## Quick start

Add plugin to *project/plugins.sbt*:

```scala

resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/groups/public"

addSbtPlugin("com.ebiznext.sbt.plugins" % "sbt-cxf-wsdl2java" % "0.1.3")
```

For *.sbt* build definitions, inject the plugin settings in *build.sbt*:

```scala
seq(cxf.settings :_*)
```

For *.scala* build definitions, inject the plugin settings in *Build.scala*:

```scala
Project(..., settings = Project.defaultSettings ++ com.ebiznext.sbt.plugins.CxfWsdl2JavaPlugin.cxf.settings)
```

## Configuration

Plugin keys are located in `com.ebiznext.sbt.plugins.CxfWsdl2JavaPlugin.Keys`

### Add Wsdls

```scala
lazy val wsclientPackage := "com.ebiznext.sbt.sample"

cxf.wsdls := Seq(
      cxf.Wsdl((resourceDirectory in Compile).value / "Sample.wsdl", Seq("-p",  wsclientPackage), "unique wsdl id"),
      ...
)
```

## Commands

```~wsdl2java``` To automatically generate source code when a wsdl is changed

TODO https://gist.github.com/meiwin/2779731
