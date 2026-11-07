import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  id("retainx.base")
  id("retainx.publishing")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.dependency.guard)
}

kotlin {
  // Public API dumps in api/, checked by `checkKotlinAbi` and updated with `updateKotlinAbi`
  @OptIn(ExperimentalAbiValidation::class) abiValidation()

  android {
    namespace = "com.retainx.store"
    withHostTest { isIncludeAndroidResources = true }
  }

  jvm()

  linuxArm64()
  linuxX64()
  mingwX64()

  iosArm64()
  iosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosSimulatorArm64()
  tvosArm64()
  tvosSimulatorArm64()
  macosArm64()

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

  // `shared` groups the targets with AndroidX ViewModel and Compose UI tests
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
    commonMain.dependencies { api(libs.compose.runtime.retain) }
    val sharedMain =
      maybeCreate("sharedMain").apply {
        // ViewModel doesn't have artifacts for linux, tvOS, watchOS, or Windows
        dependencies { implementation(libs.lifecycle.viewmodel.compose) }
      }
    iosMain { dependsOn(sharedMain) }
    macosMain { dependsOn(sharedMain) }
    webMain { dependsOn(sharedMain) }

    val sharedTest =
      maybeCreate("sharedTest").apply {
        dependencies {
          implementation(libs.kotlin.test)
          implementation(libs.compose.ui.test)
        }
      }
    iosTest { dependsOn(sharedTest) }
    macosTest { dependsOn(sharedTest) }
    jsTest { dependsOn(sharedTest) }
    wasmJsTest { dependsOn(sharedTest) }
    getByName("androidHostTest").dependencies {
      // Newer than the transitive version from ui-test, which breaks on recent SDKs
      implementation(libs.androidx.test.espresso.core)
      implementation(libs.robolectric)
    }
    jvmTest.dependencies { implementation(compose.desktop.currentOs) }
  }
}

// Baselines for every published target's dependencies. Update with `dependencyGuardBaseline`.
dependencyGuard {
  listOf("android", "jvm", "js", "wasmJs").forEach { configuration("${it}RuntimeClasspath") }
  listOf(
      "iosArm64",
      "iosSimulatorArm64",
      "linuxArm64",
      "linuxX64",
      "macosArm64",
      "mingwX64",
      "tvosArm64",
      "tvosSimulatorArm64",
      "watchosArm32",
      "watchosArm64",
      "watchosSimulatorArm64",
    )
    .forEach { configuration("${it}CompileKlibraries") }
}
