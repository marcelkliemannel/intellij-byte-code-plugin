import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.date
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  // See bundled version: https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
  kotlin("jvm") version "1.9.10"
  id("org.jetbrains.intellij.platform") version "2.0.0"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("org.jetbrains.changelog") version "2.2.0"
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
val asmVersion = "9.7"

val shadowAsmJar = tasks.create("shadowAsmJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
  group = "shadow"
  relocate("org.objectweb.asm", "dev.turingcomplete.intellijbytecodeplugin.org.objectweb.asm")
  configurations = listOf(asm)
  archiveClassifier.set("asm")
  exclude { file -> file.name == "module-info.class" }
  manifest {
    attributes("Asm-Version" to asmVersion)
  }
}

dependencies {
  intellijPlatform {
    val platformVersion = properties("platformVersion")
    create(properties("platform"), platformVersion, platformVersion == "LATEST-EAP-SNAPSHOT")

    bundledPlugins(properties("platformBundledPlugins").split(','))

    instrumentationTools()
    pluginVerifier()
    zipSigner()

    testFramework(TestFrameworkType.Bundled)
  }

  api(shadowAsmJar.outputs.files)

  asm("org.ow2.asm:asm:$asmVersion")
  asm("org.ow2.asm:asm-analysis:$asmVersion")
  asm("org.ow2.asm:asm-util:$asmVersion")
  asm("org.ow2.asm:asm-commons:$asmVersion")

  implementation("org.apache.commons:commons-text:1.11.0")

  testImplementation("org.assertj:assertj-core:3.24.2")

  val jUnit5Version = "5.10.1"
  testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnit5Version")
  testImplementation("org.junit.jupiter:junit-jupiter-params:$jUnit5Version")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnit5Version")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:$jUnit5Version")
  testImplementation("junit:junit:4.13.2")

  // Used for test data
  testImplementation("org.codehaus.groovy:groovy:3.0.16")
  testImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.10")
  testImplementation("org.apache.commons:commons-lang3:3.12.0")
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

changelog {
  val projectVersion = project.version as String
  version.set(projectVersion)
  header.set("[$projectVersion] - ${date()}")
  groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
  runIde {
    // Enable to test Kotlin K2 beta mode
    // systemProperty("idea.kotlin.plugin.use.k2", "true")
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "17"
    }
  }

  withType<Test> {
    useJUnitPlatform()
  }
}