// Copyright (C) 2025 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Ported from Circuit's `ProduceAndCollectAsRetainedStateTest`, plus the other produce/collect
 * APIs.
 */
@OptIn(ExperimentalTestApi::class)
class ProduceRetainStateTest : BaseComposeTest() {

  @Test
  fun retainsStateAcrossConfigurationChanges() = runComposeUiTest {
    val harness = RecreationHarness(this)
    // Only emits on the first collection, so a value after recreation must have been retained
    var collections = 0
    val testFlow = flow {
      if (++collections == 1) emit("test_value") else awaitCancellation()
    }
    var state = ""
    harness.setContent {
      state = produceAndCollectAsRetainState(initial = "", producer = { testFlow }).value
    }
    assertEquals("test_value", state)

    harness.recreate()

    assertEquals("test_value", state)
    assertEquals(2, collections)
    harness.dispose()
  }

  @Test
  fun resetsStateWhenInputsChange() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var producerCallCount = 0
    var input by mutableStateOf("input1")
    val stableProducer: suspend () -> Flow<String> = {
      producerCallCount++
      flowOf("value_$producerCallCount")
    }
    var state = ""
    harness.setContent {
      state =
        produceAndCollectAsRetainState(input, initial = "initial", producer = stableProducer).value
    }
    assertEquals("value_1", state)

    input = "input2"
    waitForIdle()

    assertEquals("value_2", state)
    harness.dispose()
  }

  @Test
  fun usesInitialValue() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var state = ""
    harness.setContent {
      state =
        produceAndCollectAsRetainState(
            initial = "initial_value",
            producer = { emptyFlow<String>() },
          )
          .value
    }
    assertEquals("initial_value", state)
    harness.dispose()
  }

  @Test
  fun handlesMultipleEmissions() = runComposeUiTest {
    val harness = RecreationHarness(this)
    val testFlow = flow {
      emit(1)
      emit(2)
      emit(3)
    }
    var state = 0
    harness.setContent {
      state = produceAndCollectAsRetainState(initial = 0, producer = { testFlow }).value
    }
    assertEquals(3, state)
    harness.dispose()
  }

  @Test
  fun worksWithNoExplicitInputs() = runComposeUiTest {
    val harness = RecreationHarness(this)
    val testFlow = flow { emit("test_value") }
    var state = ""
    harness.setContent {
      state = produceAndCollectAsRetainState(initial = "initial", producer = { testFlow }).value
    }
    assertEquals("test_value", state)

    // The flow collection restarts, but it emits the same value
    harness.recreate()

    assertEquals("test_value", state)
    harness.dispose()
  }

  @Test
  fun behavesLikeRegularCollectAsStateWithoutStore() = runComposeUiTest {
    val harness = RecreationHarness(this).apply { canRetain = false }
    var emissionCount = 0
    val testFlow = flow { emit("emission_${++emissionCount}") }
    var state = ""
    harness.setContent {
      state = produceAndCollectAsRetainState(initial = "initial", producer = { testFlow }).value
    }
    assertEquals("emission_1", state)

    harness.recreate()

    // Without a store, state should collect the next value emitted by the flow
    assertEquals("emission_2", state)
    harness.dispose()
  }

  @Test
  fun clearsStateWhenCompositionLeaves() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var showContent by mutableStateOf(true)
    var emissionCount = 0
    val testFlow = flow { emit("emission_${++emissionCount}") }
    var state: String? = null
    harness.setContent {
      if (showContent) {
        state = produceAndCollectAsRetainState(initial = "initial", producer = { testFlow }).value
      }
    }
    assertEquals("emission_1", state)

    // Hide the content, which removes it from composition while the store is still composed
    showContent = false
    waitForIdle()
    state = null

    showContent = true
    waitForIdle()

    // State was cleared, so the flow ran again
    assertEquals("emission_2", state)
    harness.dispose()
  }

  @Test
  fun resetsStateWhenProducerInstanceChanges() = runComposeUiTest {
    // A new lambda per value, so each producer is a distinct instance
    fun producerFor(value: String): suspend () -> Flow<String> = { flow { emit(value) } }

    val harness = RecreationHarness(this)
    var currentProducer by mutableStateOf(producerFor("producer1"))
    var state = ""
    harness.setContent {
      state = produceAndCollectAsRetainState(initial = "initial", producer = currentProducer).value
    }
    assertEquals("producer1", state)

    currentProducer = producerFor("producer2")
    waitForIdle()

    assertEquals("producer2", state)
    harness.dispose()
  }

  @Test
  fun stateFlowStartsWithCurrentValueAndRetainsAcrossRecreation() = runComposeUiTest {
    val harness = RecreationHarness(this)
    val stateFlow = MutableStateFlow("first")
    var state = ""
    harness.setContent { state = stateFlow.collectAsRetainState().value }
    assertEquals("first", state)

    stateFlow.value = "second"
    waitForIdle()
    assertEquals("second", state)

    harness.recreate()
    assertEquals("second", state)
    harness.dispose()
  }

  @Test
  fun flowStartsWithInitialAndRetainsAcrossRecreation() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var collections = 0
    val testFlow = flow {
      if (++collections == 1) emit("emitted") else awaitCancellation()
    }
    var state = ""
    harness.setContent { state = testFlow.collectAsRetainState(initial = "initial").value }
    assertEquals("emitted", state)

    harness.recreate()

    assertEquals("emitted", state)
    harness.dispose()
  }

  @Test
  fun produceRetainStateRelaunchesWhenKeyChanges() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var key by mutableStateOf(1)
    val launches = mutableListOf<String>()
    val states = mutableMapOf<String, Int>()
    harness.setContent {
      states["none"] =
        produceRetainState(0) {
            launches += "none"
            value = 1
          }
          .value
      states["key1"] =
        produceRetainState(0, key) {
            launches += "key1"
            value = key
          }
          .value
      states["key2"] =
        produceRetainState(0, key, "b") {
            launches += "key2"
            value = key
          }
          .value
      states["key3"] =
        produceRetainState(0, key, "b", "c") {
            launches += "key3"
            value = key
          }
          .value
      states["vararg"] =
        produceRetainState(0, key, "b", "c", "d") {
            launches += "vararg"
            value = key
          }
          .value
    }
    assertEquals(listOf("none", "key1", "key2", "key3", "vararg"), launches)

    launches.clear()
    key = 2
    waitForIdle()

    assertEquals(listOf("key1", "key2", "key3", "vararg"), launches)
    assertEquals(mapOf("none" to 1, "key1" to 2, "key2" to 2, "key3" to 2, "vararg" to 2), states)
    harness.dispose()
  }

  @Test
  fun produceRetainStateRunsAwaitDisposeWhenLeaving() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var show by mutableStateOf(true)
    var disposed = 0
    harness.setContent {
      if (show) {
        produceRetainState(0) { awaitDispose { disposed++ } }
      }
    }
    assertEquals(0, disposed)

    show = false
    waitForIdle()

    assertEquals(1, disposed)
    harness.dispose()
  }
}
