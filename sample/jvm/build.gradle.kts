// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  id("retainx.base")
  kotlin("jvm")
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

dependencies {
  implementation(project(":sample:shared"))
  implementation(compose.desktop.currentOs)
}

compose.desktop {
  application {
    mainClass = "com.retainx.sample.jvm.MainKt"
    nativeDistributions {
      targetFormats(
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
        org.jetbrains.compose.desktop.application.dsl.TargetFormat.Pkg,
      )
      packageName = "RetainXSample"
      packageVersion = "1.0.0"
    }
  }
}
