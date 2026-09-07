import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
  jvmToolchain(23)

  compilerOptions {
    allWarningsAsErrors.set(true)
  }

  // Android Target via AGP KMP library plugin
  android {
    namespace = "com.retainx"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
    withHostTest {
      isIncludeAndroidResources = true
    }

    lint {
      warningsAsErrors = true
      checkTestSources = true
      lintConfig = rootProject.file("config/lint/lint.xml")
    }
  }

  // JVM / Desktop Target
  jvm {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
  }

  // Native Desktop Targets
  linuxArm64()
  linuxX64()
  mingwX64()

  // Apple Targets
  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosSimulatorArm64()
  macosArm64()

  // Web / Wasm Targets
  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {
      testTask {
        useKarma {
          useChromeHeadless()
          useConfigDirectory(project.projectDir.resolve("karma.config.d"))
        }
      }
    }
    binaries.executable()
  }
  js {
    browser {
      testTask {
        useKarma {
          useChromeHeadless()
          useConfigDirectory(project.projectDir.resolve("karma.config.d"))
        }
      }
    }
    binaries.executable()
  }

  // Apply default source set hierarchy
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("shared") {
        withAndroid()
        withJvm()
      }
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
      }
    }
  }

  sourceSets {
    commonMain.dependencies {
      api(libs.compose.runtime.retain)
    }

    val sharedMain =
      maybeCreate("sharedMain").apply {
        // ViewModel doesn't have artifacts for linux, tvOS, watchOS, or Windows
        dependencies {
          implementation(libs.lifecycle.runtime.compose)
          implementation(libs.lifecycle.viewModel.compose)
        }
      }
    iosMain { dependsOn(sharedMain) }
    macosMain { dependsOn(sharedMain) }
    webMain { dependsOn(sharedMain) }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }

    val sharedTest =
      maybeCreate("sharedTest").apply {
        dependencies {
          implementation(libs.compose.ui)
          implementation(libs.compose.ui.test)
          implementation(libs.lifecycle.runtime.compose)
          implementation(libs.lifecycle.viewModel.compose)
        }
      }
    getByName("androidHostTest").apply {
      dependencies {
        implementation(libs.robolectric)
        implementation(libs.activity.compose)
        implementation(libs.androidx.test.espresso.core)
      }
    }
    jvmTest {
      dependencies {
        implementation(compose.desktop.currentOs)
      }
    }
    iosTest { dependsOn(sharedTest) }
    macosTest { dependsOn(sharedTest) }
    jsTest { dependsOn(sharedTest) }
    wasmJsTest { dependsOn(sharedTest) }
  }
}

tasks.withType<Test>().configureEach {
  jvmArgs(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.x509=ALL-UNNAMED",
  )
}

dependencies {
  lintChecks(libs.slack.compose.lint)
}

val skikoWasmRuntime = configurations.create("skikoWasmRuntime")

dependencies {
  skikoWasmRuntime("org.jetbrains.skiko:skiko-js-wasm-runtime:0.150.1")
}

abstract class UnzipSkikoWasmTask
@javax.inject.Inject
constructor(
  private val archiveOperations: ArchiveOperations,
  private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputFiles: ConfigurableFileCollection

  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @TaskAction
  fun unzip() {
    fileSystemOperations.sync {
      inputFiles.forEach { jarFile ->
        from(archiveOperations.zipTree(jarFile)) {
          include("*.wasm", "*.mjs")
        }
      }
      into(outputDir)
    }
  }
}

val unzipSkikoWasm =
  tasks.register<UnzipSkikoWasmTask>("unzipSkikoWasm") {
    inputFiles.from(skikoWasmRuntime)
    outputDir.set(layout.buildDirectory.dir("skikoWasmExtracted"))
  }

tasks
  .matching { it.name in listOf("jsTestProcessResources", "wasmJsTestProcessResources") }
  .configureEach {
    dependsOn(unzipSkikoWasm)
    (this as? Copy)?.from(unzipSkikoWasm.flatMap { it.outputDir })
  }
