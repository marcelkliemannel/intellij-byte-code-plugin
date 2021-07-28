import org.jetbrains.changelog.closure
import org.jetbrains.changelog.date

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  kotlin("jvm") version "1.5.10"
  id("org.jetbrains.intellij") version "1.1.4"
  id("com.github.johnrengelman.shadow") version "6.1.0"
  id("org.jetbrains.changelog") version "1.1.2"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenCentral()
}

val asm by configurations.creating

val shadowAsmJar = tasks.create("shadowAsmJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
  group = "shadow"
  relocate("org.objectweb.asm", "dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm")
  configurations = listOf(asm)
  archiveClassifier.set("asm")
  exclude { file -> file.name == "module-info.class" }
}

dependencies {
  api(shadowAsmJar.outputs.files)

  val asmVersion = "9.2"
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
  version = project.version as String
  header = closure { "[$version] - ${date()}" }
  groups = listOf("Added", "Changed", "Removed", "Fixed")
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
    token.set("TOKEN")
    channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
  }

  signPlugin {
    certificateChain.set("""
-----BEGIN CERTIFICATE-----
-----END CERTIFICATE-----
  """.trimIndent())

    privateKey.set("""
-----BEGIN ENCRYPTED PRIVATE KEY-----
-----END ENCRYPTED PRIVATE KEY-----
  """.trimIndent())

    password.set("PASSWORD")
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "11"
    }
  }
}