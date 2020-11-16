package com.ebiznext.sbt.plugins

import java.io.File

import sbt.Keys._
import sbt._

/**
 * @author stephane.manciot@ebiznext.com
 *
 */
object CxfWsdl2JavaPlugin extends AutoPlugin {

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  object autoImport {
    lazy val CxfConfig = config("cxf").hide

    lazy val cxfVersion = settingKey[String]("cxf version")
    lazy val wsdl2java = taskKey[Seq[File]]("Generates java files from wsdls")
    lazy val cxfWsdls = settingKey[Seq[CxfWsdl]]("wsdls to generate java files from")
    lazy val wsdl2javaDefaultArgs = settingKey[Seq[String]]("wsdl2java default arguments")
    lazy val cxfParallelExecution = settingKey[Boolean]("execute wsdl2java commands in parallel")

    case class CxfWsdl(file: File, args: Seq[String], key: String) {
      def outputDirectory(basedir: File) = new File(basedir, key).getAbsoluteFile
    }

  }

  import autoImport._

  val cxfDefaults: Seq[Def.Setting[_]] = Seq(
    cxfVersion := "3.3.4",
    libraryDependencies ++= Seq(
      "org.apache.cxf" % "cxf-tools-wsdlto-core" % cxfVersion.value % CxfConfig.name,
      "org.apache.cxf" % "cxf-tools-wsdlto-databinding-jaxb" % cxfVersion.value % CxfConfig.name,
      "org.apache.cxf" % "cxf-tools-wsdlto-frontend-jaxws" % cxfVersion.value % CxfConfig.name
    ),
    cxfWsdls := Nil,
    wsdl2javaDefaultArgs := Seq("-verbose", "-autoNameResolution", "-exsh", "true", "-fe", "jaxws21", "-client"),
    cxfParallelExecution := true
  )

  private lazy val cxfConfig = Seq(
    // initialisation de la clef correspondante au répertoire source dans lequel les fichiers générés seront copiés
    sourceManaged in CxfConfig := crossTarget.value / "cxf",
    // ajout de ce répertoire dans la liste des répertoires source à prendre en compte lors de la compilation
    managedSourceDirectories in Compile += {
      (sourceManaged in CxfConfig).value
    },
    managedClasspath in wsdl2java <<= (classpathTypes in wsdl2java, update).map { (ct, report) =>
      Classpaths.managedJars(CxfConfig, ct, report)
    },
    // définition de la tâche wsdl2java
    wsdl2java := {
      val s: TaskStreams = streams.value
      val classpath: String = (managedClasspath in wsdl2java).value.files.map(_.getAbsolutePath).mkString(System.getProperty("path.separator"))
      val basedir: File = crossTarget.value / "cxf_tmp"
      IO.createDirectory(basedir)

      def outputDir(wsdl: CxfWsdl): File = wsdl.outputDirectory(basedir)

      val wsdlColl = if (cxfParallelExecution.value) cxfWsdls.value.par else cxfWsdls.value
      val (updated, notModified) = wsdlColl.partition(wsdl =>
        wsdl.file.lastModified() > outputDir(wsdl).lastModified()
      )

      notModified.foreach(wsdl =>
        s.log.debug("Skipping " + wsdl.key)
      )

      updated.foreach { wsdl =>
        val id: String = wsdl.key
        val output = outputDir(wsdl)
        val args: Seq[String] = Seq("-d", output.getAbsolutePath) ++ wsdl2javaDefaultArgs.value ++ wsdl.args :+ wsdl.file.getAbsolutePath
        s.log.debug("Removing output directory for " + id + " ...")
        IO.delete(output)
        s.log.info("Compiling " + id)
        val cmd = Seq("java", "-cp", classpath, "-Dfile.encoding=UTF-8", "org.apache.cxf.tools.wsdlto.WSDLToJava") ++ args
        s.log.debug(cmd.toString())
        cmd ! s.log
        s.log.info("Finished " + id)
        IO.copyDirectory(output, (sourceManaged in CxfConfig).value, overwrite = true)
      }
      ((sourceManaged in CxfConfig).value ** "*.java").get
    },
    (sourceGenerators in Compile) <+= wsdl2java
  )

  override lazy val projectSettings =
    Seq(ivyConfigurations += CxfConfig) ++ cxfDefaults ++ inConfig(Compile)(cxfConfig)
}
