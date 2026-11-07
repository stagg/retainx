import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  id("retainx.base")
  id("retainx.publishing")
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
    withDeviceTest {
      instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
      api(libs.compose.runtime.saveable)
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
        implementation(libs.activity.compose)
        implementation(libs.androidx.test.espresso.core)
        implementation(libs.robolectric)
      }
    }
    getByName("androidDeviceTest").apply {
      dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.activity.compose)
        implementation(libs.androidx.test.espresso.core)
        implementation(libs.androidx.test.ext.junit)
        implementation(libs.androidx.test.runner)
        implementation(libs.compose.foundation)
        implementation(libs.compose.ui.test.manifest)
        implementation(libs.compose.ui.testing.junit)
        implementation(libs.leakcanary.android.instrumentation)
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

// The Compose resources plugin doesn't wire an output dir for the KMP Android device test variant,
// which fails task validation. This library ships no Compose resources, so point it at a build dir.
tasks
  .named { it == "copyAndroidDeviceTestComposeResourcesToAndroidAssets" }
  .configureEach {
    (property("outputDirectory") as DirectoryProperty).set(
      layout.buildDirectory.dir("generated/compose/androidDeviceTestAssets")
    )
  }
