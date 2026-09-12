// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  id("retainx.base")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
  android {
    namespace = "com.retainx.sample.shared"
    withHostTest {}
  }

  jvm()

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

configurations.configureEach {
  resolutionStrategy.dependencySubstitution {
    substitute(module("org.jetbrains.compose.collection-internal:collection"))
      .using(module("${libs.androidx.collection.get()}"))
    substitute(module("org.jetbrains.compose.annotation-internal:annotation"))
      .using(module("${libs.androidx.annotation.get()}"))
    substitute(module("org.jetbrains.androidx.savedstate:savedstate"))
      .using(module("${libs.androidx.savedstate.asProvider().get()}"))
    substitute(module("org.jetbrains.androidx.savedstate:savedstate-compose"))
      .using(module("${libs.androidx.savedstate.compose.get()}"))
    substitute(module("org.jetbrains.androidx.navigationevent:navigationevent-compose"))
      .using(module("${libs.androidx.navigationevent.compose.get()}"))
  }
}
