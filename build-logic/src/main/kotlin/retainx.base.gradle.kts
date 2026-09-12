// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
import me.stagg.retainx.buildlogic.UnzipSkikoWasmTask
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.targets.js.ir.DefaultIncrementalSyncTask
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

val catalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
val jvmTargetVersion = catalog.findVersion("jvmTarget").map { it.requiredVersion }.orElse("11")
val jdkVersion = catalog.findVersion("jdk").map { it.requiredVersion.removeSuffix("-ea").toInt() }.orElse(23)

// Java configuration
pluginManager.withPlugin("java") {
  configure<JavaPluginExtension> {
    toolchain { languageVersion.set(JavaLanguageVersion.of(jdkVersion)) }
  }

  tasks.withType<JavaCompile>().configureEach {
    options.release.set(jvmTargetVersion.toInt())
  }
}

// Kotlin toolchain configuration
pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
  configure<KotlinJvmProjectExtension> {
    jvmToolchain(jdkVersion)
  }
}

// Test configuration
tasks.withType<Test>().configureEach {
  jvmArgs(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens=java.base/java.io=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
    "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
    "--add-opens=java.base/sun.security.x509=ALL-UNNAMED",
    "--enable-native-access=ALL-UNNAMED",
    "--sun-misc-unsafe-memory-access=allow",
  )
  systemProperty("java.awt.headless", "true")
}

tasks.withType<JavaExec>().configureEach {
  jvmArgs("--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
}

// Kotlin configuration
plugins.withType<KotlinBasePlugin> {
  tasks.withType<KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
      allWarningsAsErrors.convention(true)
      when (this) {
        is KotlinJvmCompilerOptions -> {
          jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
          freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xassertions=jvm",
            "-Xemit-jvm-type-annotations",
            "-jvm-default=no-compatibility",
            "-Xjspecify-annotations=strict",
          )
        }
      }

      progressiveMode.set(true)
    }
  }

  if (!project.path.startsWith(":sample") && !project.path.startsWith(":samples")) {
    tasks.withType<KotlinCompilationTask<*>>().configureEach {
      if (!name.contains("Test")) {
        compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
      }
    }
  }
}

// KMP configuration
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
  configure<KotlinMultiplatformExtension> {
    jvmToolchain(jdkVersion)

    targets.configureEach {
      compilations.configureEach {
        compileTaskProvider.configure {
          compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
        }
      }
    }
  }

  // Workaround for missing task dependency in WASM
  val executableCompileSyncTasks = tasks.withType(DefaultIncrementalSyncTask::class.java)
  executableCompileSyncTasks.configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
  tasks.withType(KotlinJsTest::class.java).configureEach {
    mustRunAfter(executableCompileSyncTasks)
  }

  // Skiko WASM runtime unpacking workaround for JS/WASM tests
  catalog.findLibrary("skiko-js-wasm-runtime").ifPresent { skikoRuntime ->
    val skikoWasmRuntime = configurations.maybeCreate("skikoWasmRuntime")
    dependencies.add("skikoWasmRuntime", skikoRuntime)

    val unzipSkikoWasm =
      tasks.register<UnzipSkikoWasmTask>("unzipSkikoWasm") {
        inputFiles.from(skikoWasmRuntime)
        outputDir.set(layout.buildDirectory.dir("skikoWasmExtracted"))
      }

    tasks
      .matching { it.name in listOf("jsTestProcessResources", "wasmJsTestProcessResources") }
      .configureEach {
        dependsOn(unzipSkikoWasm)
        (this as? Copy)?.from(unzipSkikoWasm.flatMap { it.outputDir })
      }
  }
}

// Android auto-apply
pluginManager.withPlugin("com.android.kotlin.multiplatform.library") {
  apply(plugin = "retainx.android")
}

pluginManager.withPlugin("com.android.application") { apply(plugin = "retainx.android") }
