// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val compileSdkVersion = catalog.findVersion("android-compileSdk").get().requiredVersion.toInt()
val targetSdkVersion = catalog.findVersion("android-targetSdk").get().requiredVersion.toInt()
val minSdkVersion = catalog.findVersion("android-minSdk").get().requiredVersion.toInt()
val jvmTargetVersion = catalog.findVersion("jvmTarget").map { it.requiredVersion }.orElse("11")

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
          val lintXml = rootProject.file("config/lint/lint.xml")
          if (lintXml.exists()) {
            lintConfig = lintXml
          }
        }
      }
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
