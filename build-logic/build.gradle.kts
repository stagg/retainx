// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  `kotlin-dsl`
}

kotlin {
  jvmToolchain(libs.versions.jdk.get().removeSuffix("-ea").toInt())
  compilerOptions { allWarningsAsErrors = true }
}

dependencies {
  compileOnly(libs.kotlin.gradle.plugin)
  compileOnly(libs.agp)
  compileOnly(libs.maven.publish.gradle.plugin)
}
