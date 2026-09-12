// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
plugins {
  id("retainx.base")
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "com.retainx.sample.android"

  defaultConfig {
    applicationId = "com.retainx.sample.android"
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(project(":sample:shared"))
  implementation(libs.activity.compose)
  implementation(libs.compose.ui)
  implementation(libs.compose.foundation)
  implementation(libs.compose.material3)
  implementation(libs.compose.ui.tooling.preview)
}
