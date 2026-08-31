// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.retain.ForgetfulRetainedValuesStore
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.retain.retain as androidxRetain
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
class RetainedValuesStoreProviderTest {

  @Test
  fun explicitOwnerRetainsAcrossRootCompositionRecreation() {
    val owner = RetainedValuesStoreOwner()
    val initializer = TrackingValueInitializer()
    try {
      val first = runSingleProviderHost(owner, ThrowingViewModelStoreOwner, initializer)
      assertEquals(0, first.retiredCount)

      val second = runSingleProviderHost(owner, ThrowingViewModelStoreOwner, initializer)

      assertSame(first, second)
      assertEquals(1, initializer.count)
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
  fun automaticOwnerRetainsAcrossRootCompositionRecreation() {
    val viewModelStoreOwner = TestViewModelStoreOwner()
    val initializer = TrackingValueInitializer()
    try {
      val first = runSingleProviderHost(null, viewModelStoreOwner, initializer)
      val second = runSingleProviderHost(null, viewModelStoreOwner, initializer)

      assertSame(first, second)
      assertEquals(1, initializer.count)
      assertEquals(0, first.retiredCount)

      viewModelStoreOwner.viewModelStore.clear()
      assertEquals(1, first.retiredCount)
    } finally {
      viewModelStoreOwner.viewModelStore.clear()
    }
  }

  @Test
  fun existingStoreTakesPrecedenceBeforeOwnerResolution() {
    val existingStore = ManagedRetainedValuesStore()
    val disposedOwner = RetainedValuesStoreOwner().also(RetainedValuesStoreOwner::dispose)
    var observedStore: Any? = null
    try {
      runComposeUiTest {
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
      }
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
  fun siblingProvidersRetainIndependently() {
    val viewModelStoreOwner = TestViewModelStoreOwner()
    val firstInitializer = TrackingValueInitializer()
    val secondInitializer = TrackingValueInitializer()
    try {
      val firstHost =
        runSiblingProviderHost(viewModelStoreOwner, firstInitializer, secondInitializer)
      val secondHost =
        runSiblingProviderHost(viewModelStoreOwner, firstInitializer, secondInitializer)

      assertSame(firstHost.first, secondHost.first)
      assertSame(firstHost.second, secondHost.second)
      assertNotSame(firstHost.second, firstHost.first)
      assertEquals(1, firstInitializer.count)
      assertEquals(1, secondInitializer.count)

      viewModelStoreOwner.viewModelStore.clear()
      assertEquals(1, firstHost.first.retiredCount)
      assertEquals(1, firstHost.second.retiredCount)
    } finally {
      viewModelStoreOwner.viewModelStore.clear()
    }
  }

  @Test
  fun repeatedProvidersAtTheSamePositionRetainIndependently() {
    val owner = RetainedValuesStoreOwner()
    val initializers = listOf(TrackingValueInitializer(), TrackingValueInitializer())
    try {
      val firstHost = runRepeatedProviderHost(owner, initializers)
      val secondHost = runRepeatedProviderHost(owner, initializers)

      assertSame(firstHost[0], secondHost[0])
      assertSame(firstHost[1], secondHost[1])
      assertNotSame(firstHost[1], firstHost[0])
      assertEquals(1, initializers[0].count)
      assertEquals(1, initializers[1].count)

      owner.dispose()
      assertEquals(1, firstHost[0].retiredCount)
      assertEquals(1, firstHost[1].retiredCount)
    } finally {
      owner.dispose()
    }
  }

  @Test
  fun separateOwnersPreserveIndependentRootsWhenRecreatedInDifferentOrder() {
    val firstOwner = RetainedValuesStoreOwner()
    val secondOwner = RetainedValuesStoreOwner()
    val firstInitializer = TrackingValueInitializer()
    val secondInitializer = TrackingValueInitializer()
    try {
      val firstRoot =
        runSingleProviderHost(firstOwner, ThrowingViewModelStoreOwner, firstInitializer)
      val secondRoot =
        runSingleProviderHost(secondOwner, ThrowingViewModelStoreOwner, secondInitializer)

      val recreatedSecondRoot =
        runSingleProviderHost(secondOwner, ThrowingViewModelStoreOwner, secondInitializer)
      val recreatedFirstRoot =
        runSingleProviderHost(firstOwner, ThrowingViewModelStoreOwner, firstInitializer)

      assertSame(firstRoot, recreatedFirstRoot)
      assertSame(secondRoot, recreatedSecondRoot)
      assertEquals(1, firstInitializer.count)
      assertEquals(1, secondInitializer.count)
    } finally {
      firstOwner.dispose()
      secondOwner.dispose()
    }
  }

  private fun runSingleProviderHost(
    owner: RetainedValuesStoreOwner?,
    viewModelStoreOwner: ViewModelStoreOwner,
    initializer: TrackingValueInitializer,
  ): TrackingValue {
    lateinit var value: TrackingValue
    runComposeUiTest {
      setContent {
        CompositionLocalProvider(
          LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
          LocalViewModelStoreOwner provides viewModelStoreOwner,
        ) {
          SingleProviderContent(owner, initializer) { value = it }
        }
      }
      waitForIdle()
    }
    return value
  }

  private fun runSiblingProviderHost(
    viewModelStoreOwner: ViewModelStoreOwner,
    firstInitializer: TrackingValueInitializer,
    secondInitializer: TrackingValueInitializer,
  ): Pair<TrackingValue, TrackingValue> {
    lateinit var first: TrackingValue
    lateinit var second: TrackingValue
    runComposeUiTest {
      setContent {
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
      waitForIdle()
    }
    return first to second
  }

  private fun runRepeatedProviderHost(
    owner: RetainedValuesStoreOwner,
    initializers: List<TrackingValueInitializer>,
  ): List<TrackingValue> {
    val values = arrayOfNulls<TrackingValue>(initializers.size)
    runComposeUiTest {
      setContent {
        CompositionLocalProvider(
          LocalRetainedValuesStore provides ForgetfulRetainedValuesStore,
          LocalViewModelStoreOwner provides ThrowingViewModelStoreOwner,
        ) {
          RepeatedProviderContent(owner, initializers) { index, value ->
            values[index] = value
          }
        }
      }
      waitForIdle()
    }
    return values.map(::requireNotNull)
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
