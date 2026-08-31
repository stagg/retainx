// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
package com.retainx.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CounterState(val id: String) : RetainObserver {
  var count by mutableStateOf(0)
  var retainedCount by mutableStateOf(0)
  var retiredCount by mutableStateOf(0)
  var unusedCount by mutableStateOf(0)

  override fun onRetained() {
    retainedCount++
  }

  override fun onEnteredComposition() {}

  override fun onExitedComposition() {}

  override fun onRetired() {
    retiredCount++
  }

  override fun onUnused() {
    unusedCount++
  }
}

@Composable
fun App() {
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      var showChild by remember { mutableStateOf(true) }

      Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = "RetainX Demo",
          fontSize = 28.sp,
          style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
          Button(onClick = { showChild = !showChild }) {
            Text(if (showChild) "Hide Scoped Child" else "Show Scoped Child")
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (showChild) {
          CounterCard()
        } else {
          Text(
            text = "Child is currently detached. Retained state persists!",
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
  }
}

@Composable
fun CounterCard() {
  val state =
    retain("sample_counter") {
      CounterState("counter-1")
    }

  Card(modifier = Modifier.fillMaxWidth(0.6f).padding(16.dp)) {
    Column(
      modifier = Modifier.padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "Retained Count: ${state.count}",
        fontSize = 22.sp,
        style = MaterialTheme.typography.titleMedium,
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text =
          "Retained callbacks: onRetained=${state.retainedCount}, onCleared=${state.retiredCount}",
        style = MaterialTheme.typography.bodySmall,
      )

      Spacer(modifier = Modifier.height(16.dp))

      Row {
        Button(onClick = { state.count++ }) {
          Text("Increment")
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = { state.count = 0 }) {
          Text("Reset")
        }
      }
    }
  }
}
