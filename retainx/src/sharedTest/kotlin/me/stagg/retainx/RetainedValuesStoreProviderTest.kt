// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.ForgetfulRetainedValuesStore
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain as androidxRetain
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class RetainedValuesStoreProviderTest : BaseComposeTest() {

  @Test
  fun explicitOwnerRetainsAcrossRootCompositionRecreation() = runComposeUiTest {
    val owner = RetainedValuesStoreOwner()
    val initializer = TrackingValueInitializer()
    try {
      var showContent by mutableStateOf(false)
      var current: TrackingValue? = null

      setContent {
        if (showContent) {
          CompositionLocalProvider(
            LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
            LocalViewModelStoreOwner provides ThrowingViewModelStoreOwner,
          ) {
            SingleProviderContent(owner, initializer) { current = it }
          }
        }
      }

      showContent = true
      waitForIdle()
      val first = requireNotNull(current)
      assertEquals(0, first.retiredCount)

      showContent = false
      waitForIdle()
      current = null
      showContent = true
      waitForIdle()
      val second = requireNotNull(current)

      assertSame(first, second)
      assertEquals(1, initializer.count)

      showContent = false
      waitForIdle()
      assertEquals(2, first.enteredCount)
      assertEquals(2, first.exitedCount)
      assertEquals(0, first.retiredCount)

      owner.dispose()
      assertEquals(1, first.retiredCount)
    } finally {
      owner.dispose()
    }
  }

  @Test
  fun automaticOwnerRetainsAcrossRootCompositionRecreation() = runComposeUiTest {
    val viewModelStoreOwner = TestViewModelStoreOwner()
    val initializer = TrackingValueInitializer()
    try {
      var showContent by mutableStateOf(false)
      var current: TrackingValue? = null

      setContent {
        if (showContent) {
          CompositionLocalProvider(
            LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
            LocalViewModelStoreOwner provides viewModelStoreOwner,
          ) {
            SingleProviderContent(null, initializer) { current = it }
          }
        }
      }

      showContent = true
      waitForIdle()
      val first = requireNotNull(current)

      showContent = false
      waitForIdle()
      current = null
      showContent = true
      waitForIdle()
      val second = requireNotNull(current)

      assertSame(first, second)
      assertEquals(1, initializer.count)
      assertEquals(0, first.retiredCount)

      showContent = false
      waitForIdle()

      viewModelStoreOwner.viewModelStore.clear()
      assertEquals(1, first.retiredCount)
    } finally {
      viewModelStoreOwner.viewModelStore.clear()
    }
  }

  @Test
  fun existingStoreTakesPrecedenceBeforeOwnerResolution() = runComposeUiTest {
    val existingStore = ManagedRetainedValuesStore()
    val disposedOwner = RetainedValuesStoreOwner().also(RetainedValuesStoreOwner::dispose)
    var observedStore: Any? = null
    try {
      setContent {
        CompositionLocalProvider(
          LocalRetainedValuesStore provides existingStore,
          LocalViewModelStoreOwner provides ThrowingViewModelStoreOwner,
        ) {
          RetainedValuesStoreProvider(owner = disposedOwner) {
            val currentStore = LocalRetainedValuesStore.current
            SideEffect { observedStore = currentStore }
          }
        }
      }
      waitForIdle()
      assertSame(existingStore, observedStore)
    } finally {
      existingStore.dispose()
    }
  }

  @Test
  fun missingOwnerThrows() {
    val exception =
      assertFailsWith<IllegalStateException> {
        resolveRetainedValuesStoreOwner(owner = null, automaticOwner = { null })
      }
    assertContains(exception.message ?: "", "requires a RetainedValuesStoreOwner.")
  }

  @Test
  fun siblingProvidersRetainIndependently() = runComposeUiTest {
    val viewModelStoreOwner = TestViewModelStoreOwner()
    val firstInitializer = TrackingValueInitializer()
    val secondInitializer = TrackingValueInitializer()
    try {
      var showContent by mutableStateOf(false)
      var first: TrackingValue? = null
      var second: TrackingValue? = null

      setContent {
        if (showContent) {
          CompositionLocalProvider(
            LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
            LocalViewModelStoreOwner provides viewModelStoreOwner,
          ) {
            SiblingProviderContent(
              firstInitializer = firstInitializer,
              secondInitializer = secondInitializer,
              onFirst = { first = it },
              onSecond = { second = it },
            )
          }
        }
      }

      showContent = true
      waitForIdle()
      val firstHost1 = requireNotNull(first)
      val secondHost1 = requireNotNull(second)

      showContent = false
      waitForIdle()
      first = null
      second = null
      showContent = true
      waitForIdle()
      val firstHost2 = requireNotNull(first)
      val secondHost2 = requireNotNull(second)

      assertSame(firstHost1, firstHost2)
      assertSame(secondHost1, secondHost2)
      assertNotSame(secondHost1, firstHost1)
      assertEquals(1, firstInitializer.count)
      assertEquals(1, secondInitializer.count)

      showContent = false
      waitForIdle()

      viewModelStoreOwner.viewModelStore.clear()
      assertEquals(1, firstHost1.retiredCount)
      assertEquals(1, secondHost1.retiredCount)
    } finally {
      viewModelStoreOwner.viewModelStore.clear()
    }
  }

  @Test
  fun repeatedProvidersAtTheSamePositionRetainIndependently() = runComposeUiTest {
    val owner = RetainedValuesStoreOwner()
    val initializers = listOf(TrackingValueInitializer(), TrackingValueInitializer())
    try {
      var showContent by mutableStateOf(false)
      val values = arrayOfNulls<TrackingValue>(initializers.size)

      setContent {
        if (showContent) {
          CompositionLocalProvider(
            LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
            LocalViewModelStoreOwner provides ThrowingViewModelStoreOwner,
          ) {
            RepeatedProviderContent(owner, initializers) { index, value ->
              values[index] = value
            }
          }
        }
      }

      showContent = true
      waitForIdle()
      val first0 = requireNotNull(values[0])
      val first1 = requireNotNull(values[1])

      showContent = false
      waitForIdle()
      values[0] = null
      values[1] = null
      showContent = true
      waitForIdle()
      val second0 = requireNotNull(values[0])
      val second1 = requireNotNull(values[1])

      assertSame(first0, second0)
      assertSame(first1, second1)
      assertNotSame(first1, first0)
      assertEquals(1, initializers[0].count)
      assertEquals(1, initializers[1].count)

      showContent = false
      waitForIdle()

      owner.dispose()
      assertEquals(1, first0.retiredCount)
      assertEquals(1, first1.retiredCount)
    } finally {
      owner.dispose()
    }
  }

  @Test
  fun separateOwnersPreserveIndependentRootsWhenRecreatedInDifferentOrder() = runComposeUiTest {
    val firstOwner = RetainedValuesStoreOwner()
    val secondOwner = RetainedValuesStoreOwner()
    val firstInitializer = TrackingValueInitializer()
    val secondInitializer = TrackingValueInitializer()
    try {
      var currentTarget by
        mutableStateOf<Pair<RetainedValuesStoreOwner, TrackingValueInitializer>?>(null)
      var currentValue: TrackingValue? = null

      setContent {
        currentTarget?.let { (owner, initializer) ->
          CompositionLocalProvider(
            LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
            LocalViewModelStoreOwner provides ThrowingViewModelStoreOwner,
          ) {
            SingleProviderContent(owner, initializer) { currentValue = it }
          }
        }
      }

      fun render(
        owner: RetainedValuesStoreOwner,
        initializer: TrackingValueInitializer,
      ): TrackingValue {
        currentTarget = null
        waitForIdle()
        currentValue = null
        currentTarget = owner to initializer
        waitForIdle()
        return requireNotNull(currentValue)
      }

      val firstRoot = render(firstOwner, firstInitializer)
      val secondRoot = render(secondOwner, secondInitializer)

      val recreatedSecondRoot = render(secondOwner, secondInitializer)
      val recreatedFirstRoot = render(firstOwner, firstInitializer)

      assertSame(firstRoot, recreatedFirstRoot)
      assertSame(secondRoot, recreatedSecondRoot)
      assertEquals(1, firstInitializer.count)
      assertEquals(1, secondInitializer.count)
    } finally {
      firstOwner.dispose()
      secondOwner.dispose()
    }
  }
}

@Composable
private fun SingleProviderContent(
  owner: RetainedValuesStoreOwner?,
  initializer: TrackingValueInitializer,
  onValue: (TrackingValue) -> Unit,
) {
  RetainedValuesStoreProvider(owner) { RecordTrackingValue(initializer, onValue) }
}

@Composable
private fun SiblingProviderContent(
  firstInitializer: TrackingValueInitializer,
  secondInitializer: TrackingValueInitializer,
  onFirst: (TrackingValue) -> Unit,
  onSecond: (TrackingValue) -> Unit,
) {
  RetainedValuesStoreProvider { RecordTrackingValue(firstInitializer, onFirst) }
  RetainedValuesStoreProvider { RecordTrackingValue(secondInitializer, onSecond) }
}

@Composable
private fun RepeatedProviderContent(
  owner: RetainedValuesStoreOwner,
  initializers: List<TrackingValueInitializer>,
  onValue: (Int, TrackingValue) -> Unit,
) {
  initializers.forEachIndexed { index, initializer ->
    RetainedValuesStoreProvider(owner) {
      RecordTrackingValue(initializer) { value -> onValue(index, value) }
    }
  }
}

@Composable
private fun RecordTrackingValue(
  initializer: TrackingValueInitializer,
  onValue: (TrackingValue) -> Unit,
) {
  val value = androidxRetain { initializer.create() }
  SideEffect { onValue(value) }
}

private class TrackingValueInitializer {
  var count = 0

  fun create(): TrackingValue {
    count++
    return TrackingValue()
  }
}

private class TrackingValue : RetainObserver {
  var enteredCount = 0
  var exitedCount = 0
  var retiredCount = 0

  override fun onRetained() = Unit

  override fun onEnteredComposition() {
    enteredCount++
  }

  override fun onExitedComposition() {
    exitedCount++
  }

  override fun onRetired() {
    retiredCount++
  }

  override fun onUnused() = Unit
}

private class TestViewModelStoreOwner : ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
}

private object ThrowingViewModelStoreOwner : ViewModelStoreOwner {
  override val viewModelStore: ViewModelStore
    get() = error("The automatic ViewModelStoreOwner should not be used.")
}
