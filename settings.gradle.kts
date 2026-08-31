// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT

pluginManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
  }
}

rootProject.name = "retainx-root"

include(":retainx")

include(":sample:shared")

include(":sample:android")

include(":sample:jvm")
