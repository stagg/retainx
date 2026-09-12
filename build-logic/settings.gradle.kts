// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
  }
  versionCatalogs {
    maybeCreate("libs").apply {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}

rootProject.name = "build-logic"
