
if (binding.variables.containsKey("_grails_batch_war_package_called")) {
  return
}

_grails_batch_war_package_called = true

scriptEnv = "production"
//ant.property(name:"grails.war.exploded", value: "true")

includeTargets << new File("${batchLauncherPluginDir}/scripts/_BatchInit.groovy")
includeTargets << grailsScript("_GrailsWar")

def props = binding.batch

target(_batchWar: "") {
  war()

  def warDir = new File(warName.replace(".war", "/war"))
  def batchDir = warDir.parentFile
  def appName = grailsAppName
  def appVersion = metadata.getApplicationVersion()
  //def appName = batchDir.name
  def templatesSourceDir = new File("${basedir}/src/templates/batch")

  ant.mkdir(dir: batchDir.absolutePath)

  def libDirName = "war/WEB-INF/lib"
  def classesDirName = "war/WEB-INF/classes"

  def bootstrapJarName = batchDir.name + ".jar"

  ant.unjar(src: warName, dest: warDir.absolutePath)
  ant.delete(file: warName)

  ant.copy(todir: warDir.absolutePath + "/WEB-INF/lib") {
    fileset(dir: "${grailsHome}/lib") {
      include(name: "org.springframework.test-*.jar")
      include(name: "servlet-api-*.jar")
      include(name: "jta-*.jar")
    }
  }


  StringBuilder manifestClasspath = new StringBuilder()
  manifestClasspath.append(". ${classesDirName} war")

  new File("WEB-INF/lib", warDir).listFiles().each {
    manifestClasspath.append(" ${libDirName}/" + it.name)
  }

  ant.jar(destfile: batchDir.absolutePath + "/" + bootstrapJarName, baseDir: warDir.absolutePath + "/WEB-INF/classes") {
    manifest {
      attribute(name: "Class-Path", value: manifestClasspath)
      attribute(name: "Main-Class", value: props.bootstrapClassName)
    }
  }


  if (templatesSourceDir.exists()) {
    def filterTokens = [appName: appName, appVersion: appVersion, bootstrapJarName: bootstrapJarName, bootstrapClassName: props.bootstrapClassName, libDirName: libDirName, classesDirName: classesDirName]
    
    ant.copy(todir: batchDir.absolutePath, filtering: true) {
      fileset(dir: templatesSourceDir.absolutePath) {
	include(name: "*")
	exclude(name: "batch-launcher*")
      }
      filterset {
	filterTokens.each {
	  filter(token: it.key, value: it.value) 
	}
      }
    }

    ant.copy(todir: batchDir.absolutePath, filtering: true) {
      fileset(dir: templatesSourceDir.absolutePath) {
	include(name: "batch-launcher*")
      }
      globmapper(from: "batch-launcher*", to: appName + "*")
      filterset {
	filterTokens.each {
	  filter(token: it.key, value: it.value) 
	}
      }
    }    
  }


  ant.delete(dir: warDir.absolutePath + "/WEB-INF/classes") {
    include(name: "**/*")
  }


}
