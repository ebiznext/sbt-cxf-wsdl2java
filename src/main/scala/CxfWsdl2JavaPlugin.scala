package com.ebiznext.sbt.plugins

import sbt._
import sbt.Keys._
import java.io.File

/**
 * @author stephane.manciot@ebiznext.com
 *
 */
object CxfWsdl2JavaPlugin extends Plugin {

  trait Keys {

    lazy val Config = config("cxf") extend(Compile) hide

    lazy val cxfVersion = settingKey[String]("cxf version")
    lazy val wsdl2java = taskKey[Seq[File]]("Generates java files from wsdls")
    lazy val wsdls = settingKey[Seq[cxf.Wsdl]]("wsdls to generate java files from")

  }  

  private object CxfDefaults extends Keys {
    val settings = Seq(
      cxfVersion := "2.7.3",
      libraryDependencies ++= Seq[ModuleID](
        "org.apache.cxf" % "cxf-tools-wsdlto-core" % cxfVersion.value % Config.name,
        "org.apache.cxf" % "cxf-tools-wsdlto-databinding-jaxb" % cxfVersion.value % Config.name,
        "org.apache.cxf" % "cxf-tools-wsdlto-frontend-jaxws" % cxfVersion.value % Config.name
      ),
      wsdls := Nil
    )
  }

  // to avoid namespace clashes, use a nested object
  object cxf extends Keys {

    case class Wsdl(file: File, args: Seq[String], key: String) {
      def outputDirectory(basedir: File) = new File(basedir, key).getAbsoluteFile
    }

    val settings = Seq(ivyConfigurations += Config) ++ CxfDefaults.settings ++ Seq(
      // initialisation de la clef correspondante au répertoire source dans lequel les fichiers générés seront copiés 
      sourceManaged in Config := sourceManaged.value / "cxf",
      // ajout de ce répertoire dans la liste des répertoires source à prendre en compte lors de la compilation
      managedSourceDirectories in Compile += {(sourceManaged in Config).value},
      managedClasspath in wsdl2java <<= (classpathTypes in wsdl2java, update) map { (ct, report) =>
          Classpaths.managedJars(Config, ct, report)
      },
      // définition de la tâche wsdl2java
      wsdl2java := {
        val s: TaskStreams = streams.value
        val classpath : String = (((managedClasspath in wsdl2java).value).files).map(_.getAbsolutePath).mkString(System.getProperty("path.separator"))
        val basedir : File = target.value / "cxf"
        IO.createDirectory(basedir)
        wsdls.value.par.foreach { wsdl =>
          val output : File = wsdl.outputDirectory(basedir)
          if(wsdl.file.lastModified() > output.lastModified()) {
            val id : String = wsdl.key
            val args : Seq[String] = Seq("-d", output.getAbsolutePath, "-verbose", "-autoNameResolution", "-exsh", "true", "-fe", "jaxws21", "-client") ++ wsdl.args :+ wsdl.file.getAbsolutePath
            s.log.debug("Removing output directory for " + id + " ...")
            IO.delete(output)
            s.log.info("Compiling " + id)
            "java -cp \""+classpath+"\" -Dfile.encoding=UTF-8 org.apache.cxf.tools.wsdlto.WSDLToJava "+args.mkString(" ") ! s.log
            s.log.info("Finished " + id)
          }
          else{
            s.log.debug("Skipping " + wsdl.key)
          }
          IO.copyDirectory(output, (sourceManaged in Config).value, true)
        }
        ((sourceManaged in Config).value ** "*.java").get
      },
      sourceGenerators in Compile <+= wsdl2java)
  }
}
