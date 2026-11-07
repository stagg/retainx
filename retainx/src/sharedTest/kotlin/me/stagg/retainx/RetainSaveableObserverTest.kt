// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
package me.stagg.retainx

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class RetainSaveableObserverTest : BaseComposeTest() {

  @Test
  fun changingInputsRetiresPreviousValue() = runComposeUiTest {
    val store = ManagedRetainedValuesStore()
    var input by mutableStateOf(1)
    var current: Tracker? = null

    setContent {
      CompositionLocalProvider(LocalRetainedValuesStore provides store) {
        current = retainSaveable(input, saver = Tracker.NeverSaver) { Tracker(input) }
      }
    }
    waitForIdle()
    val first = requireNotNull(current)
    assertEquals(Counts(retained = 1, entered = 1), first.counts)

    input = 2
    waitForIdle()
    val second = requireNotNull(current)
    assertNotSame(first, second)
    assertEquals(2, second.id)
    assertEquals(Counts(retained = 1, entered = 1, exited = 1, retired = 1), first.counts)
    assertEquals(Counts(retained = 1, entered = 1), second.counts)
  }

  @Test
  fun retainedValueWithStaleInputsIsRetired() = runComposeUiTest {
    val store = ManagedRetainedValuesStore()
    var show by mutableStateOf(true)
    var input by mutableStateOf(1)
    var current: Tracker? = null

    setContent {
      CompositionLocalProvider(LocalRetainedValuesStore provides store) {
        if (show) {
          current = retainSaveable(input, saver = Tracker.NeverSaver) { Tracker(input) }
        }
      }
    }
    waitForIdle()
    val first = requireNotNull(current)

    show = false
    waitForIdle()
    // Kept by the store while out of composition
    assertEquals(Counts(retained = 1, entered = 1, exited = 1), first.counts)

    input = 2
    show = true
    waitForIdle()
    val second = requireNotNull(current)
    assertNotSame(first, second)
    assertEquals(Counts(retained = 1, entered = 1, exited = 1, retired = 1), first.counts)
    assertEquals(Counts(retained = 1, entered = 1), second.counts)
  }

  @Test
  fun retainedValueWithSameInputsIsReused() = runComposeUiTest {
    val store = ManagedRetainedValuesStore()
    var show by mutableStateOf(true)
    var current: Tracker? = null

    setContent {
      CompositionLocalProvider(LocalRetainedValuesStore provides store) {
        if (show) {
          current = retainSaveable(1, saver = Tracker.NeverSaver) { Tracker(1) }
        }
      }
    }
    waitForIdle()
    val first = requireNotNull(current)

    show = false
    waitForIdle()
    show = true
    waitForIdle()
    assertSame(first, current)
    assertEquals(Counts(retained = 1, entered = 2, exited = 1), first.counts)
  }
}

private data class Counts(
  val retained: Int = 0,
  val entered: Int = 0,
  val exited: Int = 0,
  val retired: Int = 0,
  val unused: Int = 0,
)

private class Tracker(val id: Int) : RetainObserver {
  var counts = Counts()
    private set

  override fun onRetained() {
    counts = counts.copy(retained = counts.retained + 1)
  }

  override fun onEnteredComposition() {
    counts = counts.copy(entered = counts.entered + 1)
  }

  override fun onExitedComposition() {
    counts = counts.copy(exited = counts.exited + 1)
  }

  override fun onRetired() {
    counts = counts.copy(retired = counts.retired + 1)
  }

  override fun onUnused() {
    counts = counts.copy(unused = counts.unused + 1)
  }

  companion object {
    val NeverSaver = Saver<Tracker, Int>(save = { null }, restore = { null })
  }
}
