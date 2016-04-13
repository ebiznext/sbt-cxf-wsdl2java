sbt-cxf-wsdl2java
=================

an sbt plugin for generating java artifacts from WSDL using cxf

## Requirements

* [SBT 0.13.5+](http://www.scala-sbt.org/)


## Quick start

Add plugin to *project/plugins.sbt*:

```scala

resolvers += "Sonatype Repository" at "https://oss.sonatype.org/content/groups/public"

addSbtPlugin("com.ebiznext.sbt.plugins" % "sbt-cxf-wsdl2java" % "0.1.5")
```

## Configuration

Plugin keys are prefixed with "cxf".

* **wsdl2javaDefaultArgs**: override the default arguments passed to wsdl2java, supply another Seq[String] of arguments.
* **cxfParallelExecution**: set to *false* to disable running wsdl2java commands in parallel. Useful if there are duplicate classes
to be generated and the output directory for multiple services are the same

### Add Wsdls

```scala
lazy val wsclientPackage := "com.ebiznext.sbt.sample"

cxfWsdls := Seq(
      CxfWsdl((resourceDirectory in Compile).value / "Sample.wsdl", Seq("-p",  wsclientPackage), "unique wsdl id"),
      ...
)
```

## Commands

```~wsdl2java``` To automatically generate source code when a wsdl is changed
