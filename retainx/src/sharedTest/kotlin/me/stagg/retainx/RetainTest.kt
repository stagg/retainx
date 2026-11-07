// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.retain.retain as androidxRetain
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

/** Keyed [retain] tests. The first group is ported from Circuit's `RetainedTest`. */
@OptIn(ExperimentalTestApi::class)
class RetainTest : BaseComposeTest() {

  @Test
  fun singleWithKey() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var remembered: MutableState<String>
    lateinit var retained: MutableState<String>
    harness.setContent {
      remembered = remember { mutableStateOf("") }
      retained = retain(key = "retainedText") { mutableStateOf("") }
    }
    remembered.value = "Text_Remember"
    retained.value = "Text_Retained"

    harness.recreate()

    assertEquals("", remembered.value)
    assertEquals("Text_Retained", retained.value)
    harness.dispose()
  }

  @Test
  fun multipleKeys() = runComposeUiTest {
    val harness = RecreationHarness(this)
    val values = mutableMapOf<String, MutableState<String>>()
    harness.setContent {
      values["retainedInt"] = retain(key = "retainedInt") { mutableStateOf("0") }
      values["retained1"] = retain(key = "retained1") { mutableStateOf("") }
      values["retained2"] = retain(key = "retained2") { mutableStateOf("") }
    }
    values.getValue("retained1").value = "Text_Retained1"
    values.getValue("retained2").value = "Text_Retained2"
    values.getValue("retainedInt").value = "2"

    harness.recreate()

    assertEquals("Text_Retained1", values.getValue("retained1").value)
    assertEquals("Text_Retained2", values.getValue("retained2").value)
    assertEquals("2", values.getValue("retainedInt").value)
    harness.dispose()
  }

  @Test
  fun nestedRegistries() = runComposeUiTest {
    val harness = RecreationHarness(this)
    val values = mutableMapOf<String, MutableState<String>>()
    harness.setContent {
      values["retained1"] = retain(key = "retained1") { mutableStateOf("") }
      val level1 = retainRetainedValuesStoreRegistry()
      level1.LocalRetainedValuesStoreProvider("registryLevel1") {
        values["retained2"] = retain(key = "retained2") { mutableStateOf("") }
        val level2 = retainRetainedValuesStoreRegistry()
        level2.LocalRetainedValuesStoreProvider("registryLevel2") {
          values["retainedInt"] = retain(key = "retainedInt") { mutableStateOf("0") }
        }
      }
    }
    values.getValue("retained1").value = "Text_Retained1"
    values.getValue("retained2").value = "Text_Retained2"
    values.getValue("retainedInt").value = "2"

    harness.recreate()

    assertEquals("Text_Retained1", values.getValue("retained1").value)
    assertEquals("Text_Retained2", values.getValue("retained2").value)
    assertEquals("2", values.getValue("retainedInt").value)
    harness.dispose()
  }

  @Test
  fun nestedRegistriesWithPopAndPushWithKeys() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var showNested: MutableState<Boolean>
    val values = mutableMapOf<String, MutableState<String>>()
    harness.setContent {
      values["retained1"] = retain(key = "retained1") { mutableStateOf("") }
      showNested = androidxRetain { mutableStateOf(false) }
      val registry = retainRetainedValuesStoreRegistry()
      // Keep the retained values store around even if showNested becomes false
      if (showNested.value) {
        registry.LocalRetainedValuesStoreProvider("registry") {
          values["retained2"] = retain(key = "retained2") { mutableStateOf("") }
        }
      }
    }
    assertEquals(null, values["retained2"])

    // Push the child content
    showNested.value = true
    waitForIdle()
    values.getValue("retained1").value = "Text_Retained1"
    values.getValue("retained2").value = "Text_Retained2"

    // Pop, then push again: the child reuses its retained content
    showNested.value = false
    waitForIdle()
    values.remove("retained2")
    showNested.value = true
    waitForIdle()
    assertEquals("Text_Retained1", values.getValue("retained1").value)
    assertEquals("Text_Retained2", values.getValue("retained2").value)

    harness.recreate()

    assertEquals("Text_Retained1", values.getValue("retained1").value)
    assertEquals("Text_Retained2", values.getValue("retained2").value)
    harness.dispose()
  }

  @Test
  fun singleInput() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var remembered: MutableState<String>
    lateinit var retained: MutableState<String>
    harness.setContent {
      remembered = remember("first input") { mutableStateOf("") }
      retained = retain("first input", key = "retainedText") { mutableStateOf("") }
    }
    remembered.value = "Text_Remember"
    retained.value = "Text_Retained"

    harness.recreate()

    // Input didn't change, so the retained value is reused
    assertEquals("", remembered.value)
    assertEquals("Text_Retained", retained.value)
    harness.dispose()
  }

  @Test
  fun changingInput() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var input by mutableStateOf("first input")
    lateinit var remembered: MutableState<String>
    lateinit var retained: MutableState<String>
    harness.setContent {
      remembered = remember(input) { mutableStateOf("") }
      retained = retain(input, key = "retainedText") { mutableStateOf("") }
    }
    remembered.value = "Text_Remember"
    retained.value = "Text_Retained"

    input = "second input"
    waitForIdle()

    // Both reset with the input change
    assertEquals("", remembered.value)
    assertEquals("", retained.value)
    harness.dispose()
  }

  @Test
  fun changingInputAcrossRecreation() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var input = "first input"
    lateinit var retained: MutableState<String>
    harness.setContent { retained = retain(input, key = "retainedText") { mutableStateOf("") } }
    retained.value = "Text_Retained"

    input = "second input"
    harness.recreate()

    assertEquals("", retained.value)
    harness.dispose()
  }

  @Test
  fun noStoreBehavesLikeRegularRemember() = runComposeUiTest {
    val harness = RecreationHarness(this).apply { canRetain = false }
    lateinit var retained: MutableState<String>
    harness.setContent { retained = retain(key = "retainedText") { mutableStateOf("") } }
    retained.value = "Text_Retained"

    harness.recreate()

    assertEquals("", retained.value)
    harness.dispose()
  }

  @Test
  fun emptyKeyThrows() = runComposeUiTest {
    val error =
      assertFailsWith<IllegalArgumentException> {
        var value: Any? = null
        setContent { value = retain(key = "") { Any() } }
        waitForIdle()
      }
    assertEquals("Retained key is empty", error.message)
  }

  @Test
  fun allowsRememberObserverThatIsAlsoRetainObserver() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var value: TestRememberAndRetainObserver
    harness.setContent { value = retain(key = "observer") { TestRememberAndRetainObserver() } }

    harness.recreate()

    // Only the RetainObserver callbacks are used
    assertEquals(0, value.rememberedCount)
    assertEquals(Counts(retained = 1, entered = 2, exited = 1), value.counts)
    harness.dispose()
  }

  @Test
  fun retainObserverCallbacksAcrossRecreation() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var value: Tracker
    harness.setContent { value = retain(key = "tracker") { Tracker() } }
    assertEquals(Counts(retained = 1, entered = 1), value.counts)

    harness.recreate()
    assertEquals(Counts(retained = 1, entered = 2, exited = 1), value.counts)
    harness.dispose()
  }

  @Test
  fun retainObserverRetiredAfterProcessDeath() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var value: Tracker
    harness.setContent { value = retain(key = "tracker") { Tracker() } }
    val original = value

    harness.recreateAfterProcessDeath()

    // The old owner is disposed, retiring what it kept
    assertNotSame(original, value)
    assertEquals(Counts(retained = 1, entered = 1, exited = 1, retired = 1), original.counts)
    assertEquals(Counts(retained = 1, entered = 1), value.counts)
    harness.dispose()
  }

  @Test
  fun retainObserverRetiredWhenContentRemoved() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var show by mutableStateOf(true)
    lateinit var value: Tracker
    harness.setContent { if (show) value = retain(key = "tracker") { Tracker() } }

    show = false
    waitForIdle()

    assertEquals(Counts(retained = 1, entered = 1, exited = 1, retired = 1), value.counts)
    harness.dispose()
  }

  @Test
  fun keyChangeWhileComposedKeepsValueUnderNewKey() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var key by mutableStateOf("first")
    lateinit var value: Any
    harness.setContent { value = retain(key = key) { Any() } }
    val original = value

    // Matches Circuit: the holder keeps its value and moves to the new key
    key = "second"
    waitForIdle()
    assertSame(original, value)

    harness.recreate()
    assertSame(original, value)
    harness.dispose()
  }

  @Test
  fun storeChangeWhileComposedRetainsIntoNewStore() = runComposeUiTest {
    val first = ManagedRetainedValuesStore()
    val second = ManagedRetainedValuesStore()
    var store by mutableStateOf(first)
    var show by mutableStateOf(true)
    lateinit var value: Any
    setContent {
      CompositionLocalProvider(LocalRetainedValuesStore provides store) {
        if (show) value = retain(key = "value") { Any() }
      }
    }
    val original = value

    store = second
    waitForIdle()
    show = false
    waitForIdle()

    // The exiting value went to the store it was last composed under
    store = first
    show = true
    waitForIdle()
    assertNotSame(original, value)

    show = false
    waitForIdle()
    store = second
    show = true
    waitForIdle()
    assertSame(original, value)
  }

  @Test
  fun sameKeyFollowsValueToNewPosition() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var useFirstPosition = true
    lateinit var value: Any
    harness.setContent {
      if (useFirstPosition) {
        key("first") { value = retain(key = "shared") { Any() } }
      } else {
        key("second") { value = retain(key = "shared") { Any() } }
      }
    }
    val original = value

    useFirstPosition = false
    harness.recreate()

    // An explicit key replaces the position, so the value follows it
    assertSame(original, value)
    harness.dispose()
  }

  @Test
  fun sameKeyAtTwoPositionsKeepsBothValues() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var first: Any
    lateinit var second: Any
    harness.setContent {
      first = retain(key = "duplicate") { Any() }
      second = retain(key = "duplicate") { Any() }
    }
    val originalFirst = first
    val originalSecond = second
    assertNotSame(originalFirst, originalSecond)

    harness.recreate()

    // Both values come back and keep their positions
    assertSame(originalFirst, first)
    assertSame(originalSecond, second)
    harness.dispose()
  }

  @Test
  fun sameKeyWithDifferentTypeRestoresStoredValue() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var useText = true
    lateinit var value: Any
    harness.setContent {
      value =
        if (useText) retain<Any>(key = "shared") { "text" } else retain<Any>(key = "shared") { 1 }
    }

    useText = false
    harness.recreate()

    // The type isn't part of the key, like Circuit, so the stored value wins
    assertEquals("text", value)
    harness.dispose()
  }
}

private class TestRememberAndRetainObserver : Tracker(), RememberObserver {
  var rememberedCount = 0

  override fun onRemembered() {
    rememberedCount++
  }

  override fun onForgotten() = Unit

  override fun onAbandoned() = Unit
}
