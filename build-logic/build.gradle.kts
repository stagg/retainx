// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  `kotlin-dsl`
}

kotlin {
  jvmToolchain(libs.versions.jdk.get().removeSuffix("-ea").toInt())
}

dependencies {
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.agp)
}
