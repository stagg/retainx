// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  jvmToolchain(23)

  compilerOptions {
    allWarningsAsErrors.set(true)
  }

  jvm {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
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

compose.desktop {
  application {
    mainClass = "com.retainx.sample.MainKt"
    nativeDistributions {
      targetFormats(
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
      )
      packageName = "RetainXSample"
      packageVersion = "1.0.0"
    }
  }
}
