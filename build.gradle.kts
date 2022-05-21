import org.jetbrains.changelog.date

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  kotlin("jvm") version "1.5.10"
  id("org.jetbrains.intellij") version "1.5.3"
  id("com.github.johnrengelman.shadow") version "6.1.0"
  id("org.jetbrains.changelog") version "1.3.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenCentral()
}

val asm : Configuration by configurations.creating

val shadowAsmJar = tasks.create("shadowAsmJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
  group = "shadow"
  relocate("org.objectweb.asm", "dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm")
  configurations = listOf(asm)
  archiveClassifier.set("asm")
  exclude { file -> file.name == "module-info.class" }
}

dependencies {
  api(shadowAsmJar.outputs.files)

  val asmVersion = "9.3"
  asm("org.ow2.asm:asm:$asmVersion")
  asm("org.ow2.asm:asm-analysis:$asmVersion")
  asm("org.ow2.asm:asm-util:$asmVersion")
  asm("org.ow2.asm:asm-commons:$asmVersion")

  testImplementation(localGroovy())
}

intellij {
  version.set(properties("platformVersion"))
  type.set(properties("platformType"))
  downloadSources.set(properties("platformDownloadSources").toBoolean())
  updateSinceUntilBuild.set(true)
  plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

changelog {
  version.set(project.version as String)
  header.set("[$version] - ${date()}")
  groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
  patchPluginXml {
    version.set(properties("pluginVersion"))
    sinceBuild.set(properties("pluginSinceBuild"))
    untilBuild.set(properties("pluginUntilBuild"))
    changeNotes.set(provider { changelog.getLatest().toHTML() })
  }

  runPluginVerifier {
    ideVersions.set(properties("pluginVerifierIdeVersions").split(',').map(String::trim).filter(String::isNotEmpty))
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token.set(project.provider { properties("jetbrains.marketplace.token") })
    channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
  }

  signPlugin {
    val jetbrainsDir = File(System.getProperty("user.home"), ".jetbrains")
    certificateChain.set(project.provider { File(jetbrainsDir, "plugin-sign-chain.crt").readText() })
    privateKey.set(project.provider { File(jetbrainsDir, "plugin-sign-private-key.pem").readText() })

    password.set(project.provider { properties("signing.password") })
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "11"
    }
  }
}