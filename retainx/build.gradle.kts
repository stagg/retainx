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

  // iOS Targets
  iosArm64()
  iosSimulatorArm64()

  // macOS Target
  macosArm64()

  // Web / Wasm Targets
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser()
  }
  js {
    browser()
  }

  // Apply default source set hierarchy
  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
      implementation(libs.kotlinx.coroutines.core)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
    }
  }
}

dependencies {
  lintChecks(libs.slack.compose.lint)
}
