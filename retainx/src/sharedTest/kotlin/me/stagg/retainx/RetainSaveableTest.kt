// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * [retainSaveable] tests. The first three are ported from Circuit's `RetainedSaveableTest` and the
 * screen tests from Circuit's `FirstPartyRetainInteropTest`.
 */
@OptIn(ExperimentalTestApi::class)
class RetainSaveableTest : BaseComposeTest() {

  @Test
  fun retainedIsUsedWhenRecreating() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var id = 0
    lateinit var data: CacheableData
    harness.setContent { data = cacheableData { CacheableData(id++) } }
    assertEquals(0, data.id)
    assertNull(data.superBigData)

    data.superBigData = "Super big data"
    harness.recreate()

    // Retained state is preserved
    assertEquals(0, data.id)
    assertEquals("Super big data", data.superBigData)
    harness.dispose()
  }

  @Test
  fun saveableIsUsedWhenRecreatingWithProcessDeath() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var id = 0
    lateinit var data: CacheableData
    harness.setContent { data = cacheableData { CacheableData(id++) } }
    data.superBigData = "Super big data"

    harness.recreateAfterProcessDeath()

    // Retained state is not preserved, but id is
    assertEquals(0, data.id)
    assertNull(data.superBigData)
    harness.dispose()
  }

  @Test
  fun saveableIsUsedWhenRetainIsNoop() = runComposeUiTest {
    val harness = RecreationHarness(this).apply { canRetain = false }
    var id = 0
    lateinit var data: CacheableData
    harness.setContent { data = cacheableData { CacheableData(id++) } }
    data.superBigData = "Super big data"

    harness.recreate()

    // Retained state is not preserved, but id is
    assertEquals(0, data.id)
    assertNull(data.superBigData)
    harness.dispose()
  }

  @Test
  fun survivesRecreationTwice() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var id = 0
    lateinit var data: CacheableData
    harness.setContent { data = cacheableData { CacheableData(id++) } }
    val original = data

    harness.recreate()
    harness.recreate()

    assertSame(original, data)
    harness.dispose()
  }

  @Test
  fun mutableStateIsRetainedAndSaved() = runComposeUiTest {
    val harness = RecreationHarness(this)
    lateinit var state: MutableState<String>
    harness.setContent {
      state = retainSaveable(stateSaver = autoSaver()) { mutableStateOf("initial") }
    }
    state.value = "changed"
    val original = state

    harness.recreate()
    assertSame(original, state)

    harness.recreateAfterProcessDeath()
    assertNotSame(original, state)
    assertEquals("changed", state.value)
    harness.dispose()
  }

  @Test
  fun mutableStateWithCustomSaverIsSaved() = runComposeUiTest {
    val harness = RecreationHarness(this)
    val pointSaver =
      Saver<Point, String>(save = { "${it.x},${it.y}" }, restore = { Point.parse(it) })
    lateinit var state: MutableState<Point>
    harness.setContent {
      state = retainSaveable(stateSaver = pointSaver) { mutableStateOf(Point(0, 0)) }
    }
    state.value = Point(3, 4)

    harness.recreateAfterProcessDeath()

    assertEquals(Point(3, 4), state.value)
    harness.dispose()
  }

  @Test
  fun customMutationPolicyRequiresCustomSaver() = runComposeUiTest {
    val error =
      assertFailsWith<IllegalArgumentException> {
        var value: Any? = null
        RecreationHarness(this).setContent {
          value = retainSaveable { mutableStateOf(1, CustomPolicy) }
        }
      }
    assertContains(error.message.orEmpty(), "custom SnapshotMutationPolicy")
  }

  @Test
  fun unsaveableValueThrows() = runComposeUiTest {
    val error =
      assertFailsWith<IllegalArgumentException> {
        var value: Any? = null
        RecreationHarness(this).setContent { value = retainSaveable { Point(1, 2) } }
      }
    assertContains(
      error.message.orEmpty(),
      "cannot be saved using the current SaveableStateRegistry",
    )
  }

  @Test
  fun unsaveableMutableStateThrows() = runComposeUiTest {
    val error =
      assertFailsWith<IllegalArgumentException> {
        var value: Any? = null
        RecreationHarness(this).setContent {
          value = retainSaveable { mutableStateOf(Point(1, 2)) }
        }
      }
    assertContains(error.message.orEmpty(), "MutableState containing Point(x=1, y=2)")
  }

  @Test
  fun explicitKeyRestoresAtNewPositionAfterProcessDeath() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var useFirstPosition = true
    lateinit var state: MutableState<String>
    harness.setContent {
      if (useFirstPosition) {
        key("first") {
          state = retainSaveable(stateSaver = autoSaver(), key = "shared") { mutableStateOf("") }
        }
      } else {
        key("second") {
          state = retainSaveable(stateSaver = autoSaver(), key = "shared") { mutableStateOf("") }
        }
      }
    }
    state.value = "saved"

    useFirstPosition = false
    harness.recreateAfterProcessDeath()

    assertEquals("saved", state.value)
    harness.dispose()
  }

  @Test
  fun keyChangeWhileComposedSavesUnderNewKey() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var key by mutableStateOf("first")
    var value = ""
    harness.setContent { value = retainSaveable(key = key) { "value" } }
    assertTrue("first" in harness.registry.performSave())

    key = "second"
    waitForIdle()

    val saved = harness.registry.performSave()
    assertEquals(setOf("second"), saved.keys)
    assertEquals("value", value)
    harness.dispose()
  }

  @Test
  fun registryChangeWhileComposedSavesIntoNewRegistry() = runComposeUiTest {
    val first = newRegistry(restored = null)
    val second = newRegistry(restored = null)
    var registry by mutableStateOf(first)
    var value = ""
    setContent {
      CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
        value = retainSaveable(key = "key") { "value" }
      }
    }
    waitForIdle()

    registry = second
    waitForIdle()

    assertEquals(emptyMap(), first.performSave())
    assertEquals(mapOf("key" to listOf<Any?>("value")), second.performSave())
    assertEquals("value", value)
  }

  @Test
  fun retainedAndSavedCopiesFollowInputChanges() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var input by mutableStateOf(1)
    lateinit var value: String
    harness.setContent { value = retainSaveable(input, key = "key") { "value $input" } }

    input = 2
    waitForIdle()

    // Both copies write out the current value
    assertEquals(mapOf("key" to listOf<Any?>("value 2")), harness.registry.performSave())
    harness.recreate()
    assertEquals("value 2", value)
    harness.recreateAfterProcessDeath()
    assertEquals("value 2", value)
    harness.dispose()
  }

  @Test
  fun savedValueIgnoresInputsAfterProcessDeath() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var input = 1
    lateinit var value: String
    harness.setContent { value = retainSaveable(input, key = "key") { "value $input" } }

    input = 2
    harness.recreateAfterProcessDeath()

    // Inputs aren't saved, like rememberSaveable, so the saved value is restored
    assertEquals("value 1", value)
    harness.dispose()
  }

  @Test
  fun retainedValueWithStaleInputsUsesInitNotSavedValue() = runComposeUiTest {
    val harness = RecreationHarness(this)
    var input = 1
    lateinit var value: String
    harness.setContent { value = retainSaveable(input, key = "key") { "value $input" } }

    input = 2
    harness.recreate()

    // The saved copy was written for the same stale inputs, so it's not used either
    assertEquals("value 2", value)
    harness.dispose()
  }

  @Test
  fun saveableRestoresAcrossScreenSwitchWithStoreProvider() = runComposeUiTest {
    val screens = ScreenHost(this, withStoreProvider = true)
    screens.setCount("A", 5)
    screens.switchTo("B", "A", "B", "A")
    assertEquals(5, screens.count("A"))
    screens.harness.dispose()
  }

  @Test
  fun saveableRestoresAcrossScreenSwitchWithoutStoreProvider() = runComposeUiTest {
    val screens = ScreenHost(this, withStoreProvider = false)
    screens.setCount("A", 5)
    screens.switchTo("B", "A")
    assertEquals(5, screens.count("A"))
    screens.harness.dispose()
  }

  @Test
  fun retainedValueSurvivesScreenSwitch() = runComposeUiTest {
    val screens = ScreenHost(this, withStoreProvider = true)
    val original = screens.box("A")
    screens.switchTo("B", "A")
    assertSame(original, screens.box("A"))
    screens.harness.dispose()
  }

  @Test
  fun retainedAndSaveableSurviveRecreation() = runComposeUiTest {
    val screens = ScreenHost(this, withStoreProvider = true)
    screens.setCount("A", 11)
    val boxBefore = screens.box("A")

    screens.harness.recreate()

    assertEquals(11, screens.count("A"))
    assertSame(boxBefore, screens.box("A"))
    screens.harness.dispose()
  }

  @Test
  fun offScreenSaveableSurvivesRecreation() = runComposeUiTest {
    val screens = ScreenHost(this, withStoreProvider = true)
    screens.setCount("A", 7)
    screens.switchTo("B")

    screens.harness.recreate()
    screens.switchTo("A")

    assertEquals(7, screens.count("A"))
    screens.harness.dispose()
  }

  @Test
  fun offScreenSaveableSurvivesProcessDeath() = runComposeUiTest {
    val screens = ScreenHost(this, withStoreProvider = true)
    screens.setCount("A", 7)
    screens.switchTo("B")

    screens.harness.recreateAfterProcessDeath()
    screens.switchTo("A")

    assertEquals(7, screens.count("A"))
    screens.harness.dispose()
  }

  @Composable
  private fun cacheableData(init: () -> CacheableData): CacheableData =
    retainSaveable(saver = CacheableData.Saver, calculation = init)
}

/**
 * The navigation shape from Circuit's `NavigableCircuitContent`: per-screen movable content, each
 * with its own retained values store and saveable scope.
 */
@OptIn(ExperimentalTestApi::class)
private class ScreenHost(
  private val test: androidx.compose.ui.test.ComposeUiTest,
  withStoreProvider: Boolean,
) {
  val harness = RecreationHarness(test)
  private val counts = mutableMapOf<String, MutableState<Int>>()
  private val boxes = mutableMapOf<String, Any>()
  private var currentScreen by mutableStateOf("A")

  init {
    harness.setContent {
      val saveableStateHolder = rememberSaveableStateHolder()
      val registry = retainRetainedValuesStoreRegistry()
      val screens = remember {
        listOf("A", "B").associateWith { name ->
          movableContentOf {
            if (withStoreProvider) {
              registry.LocalRetainedValuesStoreProvider(name) {
                saveableStateHolder.SaveableStateProvider(name) { CounterScreen(name) }
              }
            } else {
              saveableStateHolder.SaveableStateProvider(name) { CounterScreen(name) }
            }
          }
        }
      }
      screens.getValue(currentScreen).invoke()
    }
  }

  @Composable
  private fun CounterScreen(name: String) {
    counts[name] = retainSaveable(stateSaver = autoSaver()) { mutableStateOf(0) }
    boxes[name] = retain(key = "box") { Any() }
  }

  fun count(name: String): Int = counts.getValue(name).value

  fun box(name: String): Any = boxes.getValue(name)

  fun setCount(name: String, count: Int) {
    counts.getValue(name).value = count
    test.waitForIdle()
  }

  fun switchTo(vararg names: String) {
    for (name in names) {
      currentScreen = name
      test.waitForIdle()
    }
  }
}

@Stable
private class CacheableData(val id: Int) {
  var superBigData: String? by mutableStateOf(null)

  companion object {
    val Saver: Saver<CacheableData, *> = Saver({ it.id }, { CacheableData(it) })
  }
}

private data class Point(val x: Int, val y: Int) {
  companion object {
    fun parse(value: String): Point = value.split(",").let { (x, y) -> Point(x.toInt(), y.toInt()) }
  }
}

private object CustomPolicy : SnapshotMutationPolicy<Int> {
  override fun equivalent(a: Int, b: Int): Boolean = a == b
}
