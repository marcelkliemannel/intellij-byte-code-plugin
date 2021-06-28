import org.jetbrains.changelog.closure
import org.jetbrains.changelog.date

plugins {
  java
  kotlin("jvm") version "1.5.0"
  id("org.jetbrains.intellij") version "0.7.2"
  id("com.github.johnrengelman.shadow") version "6.1.0"
  id("org.jetbrains.changelog") version "1.1.2"
}

group = "dev.turingcomplete"
version = "1.0.1"

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
  implementation(kotlin("stdlib-jdk8"))

  api(shadowAsmJar.outputs.files)

  val asmVersion = "9.2"
  asm("org.ow2.asm:asm:$asmVersion")
  asm("org.ow2.asm:asm-analysis:$asmVersion")
  asm("org.ow2.asm:asm-util:$asmVersion")
  asm("org.ow2.asm:asm-commons:$asmVersion")

  testImplementation(localGroovy())
}

intellij {
  version = "2021.1"
  setPlugins("com.intellij.java")
}

changelog {
  version = project.version as String
  header = closure { "[$version] - ${date()}" }
  groups = listOf("Added", "Changed", "Removed", "Fixed")
}

tasks {
  patchPluginXml {
    changeNotes(closure { changelog.get(project.version as String).toHTML() })
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "11"
  }
}