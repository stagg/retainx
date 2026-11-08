// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val compileSdkVersion = catalog.findVersion("android-compile-sdk").get().requiredVersion.toInt()
val targetSdkVersion = catalog.findVersion("android-target-sdk").get().requiredVersion.toInt()
val minSdkVersion = catalog.findVersion("android-min-sdk").get().requiredVersion.toInt()
val jvmTargetVersion = catalog.findVersion("jvm-target").map { it.requiredVersion }.orElse("11")

/** Run with `<module>:pixel6Api34AndroidDeviceTest`. CI runs it from the device test job. */
val ManagedDeviceName = "pixel6Api34"

fun CommonExtension.configureCommonAndroid() {
  compileSdk = compileSdkVersion
}

// Android KMP Library configuration
pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
  pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    (kotlinExtension as KotlinMultiplatformExtension)
      .targets
      .withType(KotlinMultiplatformAndroidLibraryTarget::class.java)
      .configureEach {
        compileSdk = compileSdkVersion
        minSdk = minSdkVersion
        compilations.withType(KotlinMultiplatformAndroidHostTestCompilation::class.java) {
          targetSdk { release(targetSdkVersion) }
        }
        lint {
          checkTestSources = true
          checkDependencies = false
          warningsAsErrors = true
          val lintXml = rootProject.file("config/lint/lint.xml")
          if (lintXml.exists()) {
            lintConfig = lintXml
          }
        }
        // Modules with device tests get a runner and a managed device for CI
        if (file("src/androidDeviceTest").exists()) {
          withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            managedDevices {
              localDevices.create(ManagedDeviceName) {
                device = "Pixel 6"
                sdkVersion = 34
                // Automated Test Device images are headless and trimmed down for faster CI runs
                systemImageSource = "aosp-atd"
              }
            }
          }
        }
      }
  }

  // Compose Multiplatform's resources plugin doesn't wire an output dir for the KMP Android
  // device test variant, which fails task validation. Nothing here ships Compose resources, so
  // point it at a build dir.
  tasks
    .named { it == "copyAndroidDeviceTestComposeResourcesToAndroidAssets" }
    .configureEach {
      (property("outputDirectory") as DirectoryProperty).set(
        layout.buildDirectory.dir("generated/compose/androidDeviceTestAssets")
      )
    }

  catalog.findLibrary("slack-compose-lint").ifPresent { composeLint ->
    dependencies.add("lintChecks", composeLint)
  }
}

// Android Application configuration
pluginManager.withPlugin("com.android.application") {
  extensions.configure<ApplicationExtension> {
    configureCommonAndroid()

    compileOptions {
      sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
      targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
    }

    defaultConfig {
      minSdk = minSdkVersion
      targetSdk = targetSdkVersion
    }

    lint {
      checkTestSources = true
      checkDependencies = false
      warningsAsErrors = true
      val lintXml = rootProject.file("config/lint/lint.xml")
      if (lintXml.exists()) {
        lintConfig = lintXml
      }
    }
  }

  catalog.findLibrary("slack-compose-lint").ifPresent { composeLint ->
    dependencies.add("lintChecks", composeLint)
  }
}
