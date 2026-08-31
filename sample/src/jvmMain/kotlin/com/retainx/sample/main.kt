// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
package com.retainx.sample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
  Window(
    onCloseRequest = ::exitApplication,
    title = "RetainX Sample App",
    state = rememberWindowState(width = 600.dp, height = 500.dp),
  ) {
    App()
  }
}
