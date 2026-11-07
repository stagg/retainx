// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
package me.stagg.retainx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.retain.ForgetfulRetainedValuesStore
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi

/**
 * Hosts content the way an Android activity does, without needing one: a [SaveableStateRegistry]
 * for saved state and a [RetainedValuesStoreProvider] for retained values. [recreate] stands in for
 * a configuration change and [recreateAfterProcessDeath] for process death.
 */
@OptIn(ExperimentalTestApi::class)
internal class RecreationHarness(private val test: ComposeUiTest) {
  private var owner = RetainedValuesStoreOwner()
  private var isShowing by mutableStateOf(true)

  /** The registry content currently saves into. Swapped on every recreation. */
  var registry: SaveableStateRegistry by mutableStateOf(newRegistry(restored = null))
    private set

  /** When false, no retained store is installed, like a host that can't retain. */
  var canRetain: Boolean = true

  fun setContent(content: @Composable () -> Unit) {
    test.setContent {
      if (isShowing) {
        CompositionLocalProvider(
          LocalSaveableStateRegistry provides registry,
          // Hide any platform store so retention is controlled by [owner]
          LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
        ) {
          if (canRetain) RetainedValuesStoreProvider(owner, content) else content()
        }
      }
    }
    test.waitForIdle()
  }

  /** Saves state, disposes the composition, and composes it again with retained values. */
  fun recreate() = recreate(processDeath = false)

  /** Like [recreate], but retained values are lost and only saved state is restored. */
  fun recreateAfterProcessDeath() = recreate(processDeath = true)

  fun dispose() = owner.dispose()

  private fun recreate(processDeath: Boolean) {
    val saved = registry.performSave()
    isShowing = false
    test.waitForIdle()
    if (processDeath) {
      owner.dispose()
      owner = RetainedValuesStoreOwner()
    }
    registry = newRegistry(saved)
    isShowing = true
    test.waitForIdle()
  }
}

internal fun newRegistry(restored: Map<String, List<Any?>>?): SaveableStateRegistry =
  SaveableStateRegistry(restored, ::canBeSavedToBundle)

/** Approximates what an Android `Bundle` accepts, so unsaveable values fail like on device. */
private fun canBeSavedToBundle(value: Any): Boolean =
  when (value) {
    is SnapshotMutableState<*> ->
      (value.policy === neverEqualPolicy<Any?>() ||
        value.policy === structuralEqualityPolicy<Any?>() ||
        value.policy === referentialEqualityPolicy<Any?>()) &&
        value.value.let { it == null || canBeSavedToBundle(it) }
    is String,
    is Number,
    is Boolean,
    is Char -> true
    is List<*> -> value.all { it == null || canBeSavedToBundle(it) }
    is Map<*, *> ->
      value.all { (k, v) ->
        k != null && canBeSavedToBundle(k) && (v == null || canBeSavedToBundle(v))
      }
    else -> false
  }
