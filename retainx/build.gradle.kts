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
    withHostTest {}

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
    browser()
    binaries.executable()
  }
  js {
    browser()
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

    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}

dependencies {
  lintChecks(libs.slack.compose.lint)
}
