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
    lazy val cxfConfig = config("cxfConfig").hide

    lazy val cxfVersion = settingKey[String]("cxf version")
    lazy val wsdl2java = taskKey[Seq[File]]("Generates java files from wsdls")
    lazy val cxfWsdls = settingKey[Seq[CxfWsdl]]("wsdls to generate java files from")
    lazy val wsdl2javaDefaultArgs = settingKey[Seq[String]]("wsdl2java default arguments")

    case class CxfWsdl(file: File, args: Seq[String], key: String) {
      def outputDirectory(basedir: File) = new File(basedir, key).getAbsoluteFile
    }

  }

  import autoImport._

  val cxfDefaults: Seq[Def.Setting[_]] = Seq(
    cxfVersion := "3.1.2",
    libraryDependencies ++= Seq(
      "org.apache.cxf" % "cxf-tools-wsdlto-core" % cxfVersion.value % CxfConfig.name,
      "org.apache.cxf" % "cxf-tools-wsdlto-databinding-jaxb" % cxfVersion.value % CxfConfig.name,
      "org.apache.cxf" % "cxf-tools-wsdlto-frontend-jaxws" % cxfVersion.value % CxfConfig.name
    ),
    cxfWsdls := Nil,
    wsdl2javaDefaultArgs := Seq("-verbose", "-autoNameResolution", "-exsh", "true", "-fe", "jaxws21", "-client")
  )

  private lazy val cxfConfig = Seq(
    // initialisation de la clef correspondante au répertoire source dans lequel les fichiers générés seront copiés
    sourceManaged in CxfConfig := sourceManaged.value / "cxf",
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
      val classpath: String = (((managedClasspath in wsdl2java).value).files).map(_.getAbsolutePath).mkString(System.getProperty("path.separator"))
      val basedir: File = target.value / "cxf"
      IO.createDirectory(basedir)
      cxfWsdls.value.par.foreach { wsdl =>
        val output: File = wsdl.outputDirectory(basedir)
        if (wsdl.file.lastModified() > output.lastModified()) {
          val id: String = wsdl.key
          val args: Seq[String] = Seq("-d", output.getAbsolutePath) ++ wsdl2javaDefaultArgs.value ++ wsdl.args :+ wsdl.file.getAbsolutePath
          s.log.debug("Removing output directory for " + id + " ...")
          IO.delete(output)
          s.log.info("Compiling " + id)
          val cmd = Seq("java", "-cp", classpath, "-Dfile.encoding=UTF-8", "org.apache.cxf.tools.wsdlto.WSDLToJava") ++ args
          s.log.debug(cmd.toString())
          cmd ! s.log
          s.log.info("Finished " + id)
        }
        else {
          s.log.debug("Skipping " + wsdl.key)
        }
        IO.copyDirectory(output, (sourceManaged in CxfConfig).value, true)
      }
      ((sourceManaged in CxfConfig).value ** "*.java").get
    },
    sourceGenerators in Compile <+= wsdl2java
  )

  override lazy val projectSettings =
    Seq(ivyConfigurations += CxfConfig) ++ cxfDefaults ++ inConfig(Compile)(cxfConfig)
}
