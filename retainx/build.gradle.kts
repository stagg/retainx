import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

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
  // Android Target via AGP KMP library plugin
  android {
    namespace = "com.retainx"
    withHostTest {
      isIncludeAndroidResources = true
    }
  }

  // JVM / Desktop Target
  jvm()

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
