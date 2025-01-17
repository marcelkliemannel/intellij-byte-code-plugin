
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.intellij.platform)
  alias(libs.plugins.changelog)
  alias(libs.plugins.shadow)
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenCentral()

  intellijPlatform {
    defaultRepositories()
  }
}

val asm: Configuration by configurations.creating
val asmSource: Configuration by configurations.creating
val testData: Configuration by configurations.creating
configurations.named("testRuntimeOnly").get().extendsFrom(testData)

val shadowAsmJar = tasks.register<ShadowJar>("shadowAsmJar") {
  group = "shadow"
  relocate("org.objectweb.asm", "dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm")
  configurations = listOf(asm, asmSource)
  archiveClassifier.set("asm")
  exclude { file -> file.name == "module-info.class" }
  manifest {
    attributes("Asm-Version" to libs.versions.asm.get())
  }
}

dependencies {
  intellijPlatform {
    val platformVersion = properties("platformVersion")
    create(properties("platform"), platformVersion, platformVersion == "LATEST-EAP-SNAPSHOT")

    bundledPlugins(properties("platformBundledPlugins").split(','))

    pluginVerifier()
    zipSigner()

    testFramework(TestFrameworkType.Platform)
    testFramework(TestFrameworkType.JUnit5)
    testFramework(TestFrameworkType.Plugin.Java)
  }

  api(shadowAsmJar.get().outputs.files)
  asm(libs.bundles.asm)
  asm.dependencies.forEach {
    asmSource("${it.group}:${it.name}:${it.version}:sources")
  }

  implementation(libs.commons.text)

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)

  testData(libs.groovy)
  testData(libs.kotlin.stdlib)
  testData(libs.commons.lang3)
}

intellijPlatform {
  pluginConfiguration {
    version = providers.gradleProperty("pluginVersion")
    ideaVersion {
      sinceBuild = properties("pluginSinceBuild")
      untilBuild = provider { null }
    }
    changeNotes.set(provider { changelog.renderItem(changelog.get(project.version as String), Changelog.OutputType.HTML) })
  }

  signing {
    val jetbrainsDir = File(System.getProperty("user.home"), ".jetbrains")
    certificateChain.set(project.provider { File(jetbrainsDir, "plugin-sign-chain.crt").readText() })
    privateKey.set(project.provider { File(jetbrainsDir, "plugin-sign-private-key.pem").readText() })
    password.set(project.provider { properties("jetbrains.sign-plugin.password") })
  }

  publishing {
    //        dependsOn("patchChangelog")
    token.set(project.provider { properties("jetbrains.marketplace.token") })
    channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
  }

  pluginVerification {
    failureLevel.set(
      listOf(
        COMPATIBILITY_PROBLEMS, INTERNAL_API_USAGES, NON_EXTENDABLE_API_USAGES,
        OVERRIDE_ONLY_API_USAGES, MISSING_DEPENDENCIES, INVALID_PLUGIN
      )
    )

    ides {
      recommended()
    }
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(21))
  }
}

changelog {
  val projectVersion = project.version as String
  version.set(projectVersion)
  header.set("$projectVersion - ${date()}")
  groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
  runIde {
    // Enable to test Kotlin K2 beta mode
    // systemProperty("idea.kotlin.plugin.use.k2", "true")
  }

  withType<KotlinCompile> {
    compilerOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget.set(JvmTarget.JVM_21)
    }
  }

  withType<Test> {
    useJUnitPlatform()
  }
}