// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  id("com.vanniktech.maven.publish")
}

configure<MavenPublishBaseExtension> {
  publishToMavenCentral(automaticRelease = true)
}
