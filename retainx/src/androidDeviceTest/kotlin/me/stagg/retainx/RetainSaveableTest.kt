// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.ForgetfulRetainedValuesStore
import androidx.compose.runtime.retain.LocalRetainedValuesStore
import androidx.compose.runtime.retain.LocalRetainedValuesStoreProvider
import androidx.compose.runtime.retain.ManagedRetainedValuesStore
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetainedSaveableTest {

  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private var store = ManagedRetainedValuesStore()

  @Test
  fun retainedIsUsedWhenRecreating() {
    var id = 0
    lateinit var data: CacheableData

    val content =
      @Composable {
        data = retainedSaveable { CacheableData(id++) }
        BasicText(modifier = Modifier.testTag("id"), text = "${data.id}")
        BasicText(modifier = Modifier.testTag("superBigData"), text = "${data.superBigData}")
      }

    setActivityContent(content)

    // Check initial state is correct
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")

    data.superBigData = "Super big data"
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")

    scenario.recreate()

    setActivityContent(content)

    // Retained state is preserved
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")
  }

  @Test
  fun saveableIsUsedWhenRecreatingWithProcessDeath() {
    var id = 0
    lateinit var data: CacheableData

    val content =
      @Composable {
        data = retainedSaveable { CacheableData(id++) }
        BasicText(modifier = Modifier.testTag("id"), text = "${data.id}")
        BasicText(modifier = Modifier.testTag("superBigData"), text = "${data.superBigData}")
      }

    setActivityContent(content)

    // Check initial state is correct
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")

    data.superBigData = "Super big data"
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")

    // Swap in a fresh store to drop retained values, simulating process death
    val previousStore = store
    store = ManagedRetainedValuesStore()
    scenario.recreate()
    previousStore.dispose()

    setActivityContent(content)

    // Retained state is not preserved, but id is
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")
  }

  @Test
  fun saveableIsUsedWhenRetainIsNoop() {
    var id = 0
    lateinit var data: CacheableData

    fun setContent() {
      scenario.onActivity { activity ->
        activity.setContent {
          CompositionLocalProvider(LocalRetainedValuesStore provides ForgetfulRetainedValuesStore) {
            data = retainedSaveable { CacheableData(id++) }
            BasicText(modifier = Modifier.testTag("id"), text = "${data.id}")
            BasicText(modifier = Modifier.testTag("superBigData"), text = "${data.superBigData}")
          }
        }
      }
    }

    setContent()

    // Check initial state is correct
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")

    data.superBigData = "Super big data"
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("Super big data")

    scenario.recreate()

    setContent()

    // Retained state is not preserved, but id is
    composeTestRule.onNodeWithTag("id").assertTextEquals("0")
    composeTestRule.onNodeWithTag("superBigData").assertTextEquals("null")
  }

  @Composable
  private fun retainedSaveable(init: () -> CacheableData): CacheableData =
    retainSaveable(saver = CacheableData.Saver, calculation = init)

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent {
        // Hide the activity's store so retention is controlled by [store]
        CompositionLocalProvider(LocalRetainedValuesStore provides ForgetfulRetainedValuesStore) {
          LocalRetainedValuesStoreProvider(store = store, content = content)
        }
      }
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
