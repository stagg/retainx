// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
package me.stagg.retainx.buildlogic

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Extracts Skiko WebAssembly (`*.wasm`) and JavaScript module (`*.mjs`) runtime artifacts from
 * Skiko runtime JARs (such as `skiko-js-wasm-runtime`).
 *
 * The Compose Multiplatform Gradle plugin automatically unpacks Skiko runtime files for web
 * targets only when `compose.ui` is declared as a dependency in the main compilation. When Compose
 * UI is used solely within test source sets (or when running JS/Wasm browser tests), these binary
 * artifacts are not extracted by default. This task extracts the required runtime files so they can
 * be included in `jsTestProcessResources` and `wasmJsTestProcessResources`.
 */
abstract class UnzipSkikoWasmTask
@Inject
constructor(
  private val archiveOperations: ArchiveOperations,
  private val fileSystemOperations: FileSystemOperations,
) : DefaultTask() {
  /** The Skiko runtime JAR files to extract. */
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputFiles: ConfigurableFileCollection

  /** Destination directory where the extracted `.wasm` and `.mjs` files are placed. */
  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @TaskAction
  fun unzip() {
    fileSystemOperations.sync {
      inputFiles.forEach { jarFile ->
        from(archiveOperations.zipTree(jarFile)) {
          include("*.wasm", "*.mjs")
        }
      }
      into(outputDir)
    }
  }
}
