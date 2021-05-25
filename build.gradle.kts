plugins {
  java
  kotlin("jvm") version "1.5.0"
  id("org.jetbrains.intellij") version "0.7.2"
  id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "dev.turingcomplete"
version = "0.1.0-SNAPSHOT"

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

  val asmVersion = "9.1"
  asm("org.ow2.asm:asm:$asmVersion")
  asm("org.ow2.asm:asm-analysis:$asmVersion")
  asm("org.ow2.asm:asm-util:$asmVersion")
  asm("org.ow2.asm:asm-commons:$asmVersion")
}

intellij {
  version = "2021.1"
  setPlugins("com.intellij.java")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "11"
  }
}

