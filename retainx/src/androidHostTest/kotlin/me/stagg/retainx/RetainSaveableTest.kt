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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class RetainedSaveableTest : BaseComposeTest() {

  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  private var canRetainOverride: Boolean? = null

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

    // Override can retain to drop retaining values, to simulate process death
    canRetainOverride = false
    scenario.recreate()
    canRetainOverride = null

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
        content()
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
