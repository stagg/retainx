// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
import com.android.build.api.variant.HostTestBuilder

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

  // AGP's Compose Preview tasks need unit tests with compiled resources in every variant, or
  // creating them fails (and with it `./gradlew tasks`)
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

androidComponents {
  beforeVariants { variant -> variant.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true }
}

dependencies {
  implementation(project(":sample:shared"))
  implementation(libs.activity.compose)
}
