// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
package com.retainx.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CounterState(val id: String) : RetainObserver {
  var count by mutableIntStateOf(0)
  var retainedCount by mutableIntStateOf(0)
  var enterCount by mutableIntStateOf(0)
  var exitCount by mutableIntStateOf(0)

  override fun onRetained() {
    retainedCount++
  }

  override fun onEnteredComposition() {
    enterCount++
  }

  override fun onExitedComposition() {
    exitCount++
  }

  override fun onRetired() {}

  override fun onUnused() {}
}

@Composable
fun App() {
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      var showChild by retain { mutableStateOf(true) }

      Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Text(
          text = "RetainX Demo",
          style = MaterialTheme.typography.headlineMedium,
        )

        val store = LocalRetainedValuesStore.current
        Text(
          text = "${store::class.qualifiedName}",
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row {
          Button(onClick = { showChild = !showChild }) {
            Text(if (showChild) "Hide Scoped Child" else "Show Scoped Child")
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val scopedRetainStore = retain { ManagedRetainedValuesStore() }
        if (showChild) {
          LocalRetainedValuesStoreProvider(store = scopedRetainStore) {
            CounterCard()
          }
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
  val state = retain {
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
          "Retained callbacks: onRetained=${state.retainedCount}, onEnter=${state.enterCount}, onExit=${state.exitCount}",
        style = MaterialTheme.typography.bodySmall,
      )

      Spacer(modifier = Modifier.height(16.dp))

      FlowRow {
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
