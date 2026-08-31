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

  android {
    namespace = "com.retainx.sample.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()
    withHostTest {}
  }

  jvm {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
  }

  listOf(
      iosArm64(),
      iosSimulatorArm64(),
    )
    .forEach { iosTarget ->
      iosTarget.binaries.framework {
        baseName = "SampleShared"
        isStatic = true
      }
    }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      implementation(project(":retainx"))
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui)
    }

    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
    }
  }
}
