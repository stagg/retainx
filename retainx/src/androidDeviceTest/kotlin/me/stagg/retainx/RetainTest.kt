// Copyright (C) 2022 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain as androidxRetain
import androidx.compose.runtime.retain.retainRetainedValuesStoreRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import leakcanary.DetectLeaksAfterTestSuccess.Companion.detectLeaksAfterTestSuccessWrapping
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

private const val TAG_REMEMBER = "remember"
private const val TAG_RETAINED_1 = "retained1"
private const val TAG_RETAINED_2 = "retained2"
private const val TAG_RETAINED_3 = "retained3"
private const val TAG_BUTTON_SHOW = "btn_show"
private const val TAG_BUTTON_HIDE = "btn_hide"

/** Keyed [retain] tests, ported from Circuit's `RetainedTest`. */
@RunWith(AndroidJUnit4::class)
class RetainTest {

  private val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @get:Rule
  val rule =
    RuleChain.emptyRuleChain().detectLeaksAfterTestSuccessWrapping(tag = "ActivitiesDestroyed") {
      around(composeTestRule)
    }

  private val scenario: ActivityScenario<ComponentActivity>
    get() = composeTestRule.activityRule.scenario

  @Test
  fun singleWithKey() {
    val content = @Composable { KeyContent("retainedText") }
    setActivityContent(content)
    composeTestRule.onNodeWithTag(TAG_REMEMBER).performTextInput("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("Text_Remember")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_REMEMBER).assertTextContains("")
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained")
  }

  @Test fun multipleKeys() = testMultipleRetainedContent { MultipleRetains() }

  @Test fun nestedRegistries() = testMultipleRetainedContent { NestedRetains() }

  @Test
  fun nestedRegistriesWithPopAndPushWithKeys() {
    val content = @Composable { NestedRetainWithPushAndPop() }
    setActivityContent(content)

    // Assert that Retained 1 is visible & Retained 2 does not exist
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertDoesNotExist()

    // Now click the button to show the child content
    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()

    // Perform our initial text input
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained1")
    composeTestRule
      .onNodeWithTag(TAG_RETAINED_2)
      .assertIsDisplayed()
      .performTextInput("Text_Retained2")

    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")

    // Now click the button to hide the nested content (aka a pop)
    composeTestRule.onNodeWithTag(TAG_BUTTON_HIDE).performClick()

    // Assert that Retained 1 is visible & Retained 2 does not exist
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertDoesNotExist()

    // Now click the button to show the nested content again (aka a push)
    composeTestRule.onNodeWithTag(TAG_BUTTON_SHOW).performClick()

    // Assert that the child content is using the retained content
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")

    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
  }

  private fun setActivityContent(content: @Composable () -> Unit) {
    scenario.onActivity { activity ->
      activity.setContent { RetainedValuesStoreProvider(content = content) }
    }
  }

  private fun testMultipleRetainedContent(content: @Composable () -> Unit) {
    setActivityContent(content)

    composeTestRule.onNodeWithTag(TAG_RETAINED_1).performTextInput("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).performTextInput("Text_Retained2")
    composeTestRule.onNodeWithTag(TAG_RETAINED_3).performTextInput("2")
    // Check that our input worked
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
    composeTestRule.onNodeWithTag(TAG_RETAINED_3).assertTextContains("2", substring = true)
    // Restart the activity
    scenario.recreate()
    // Compose our content
    setActivityContent(content)
    // Was the text saved
    composeTestRule.onNodeWithTag(TAG_RETAINED_1).assertTextContains("Text_Retained1")
    composeTestRule.onNodeWithTag(TAG_RETAINED_2).assertTextContains("Text_Retained2")
    composeTestRule.onNodeWithTag(TAG_RETAINED_3).assertTextContains("2", substring = true)
  }
}

@Composable
private fun KeyContent(key: String) {
  var text1 by remember { mutableStateOf("") }
  var retainedText: String by retain(key = key) { mutableStateOf("") }
  Column {
    BasicTextField(
      modifier = Modifier.testTag(TAG_REMEMBER),
      value = text1,
      onValueChange = { text1 = it },
    )
    BasicTextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText,
      onValueChange = { retainedText = it },
    )
  }
}

@Composable
private fun MultipleRetains() {
  var retainedInt: Int by retain(key = "retainedInt") { mutableStateOf(0) }
  var retainedText1: String by retain(key = "retained1") { mutableStateOf("") }
  var retainedText2: String by retain(key = "retained2") { mutableStateOf("") }
  Column {
    BasicTextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
    )
    BasicTextField(
      modifier = Modifier.testTag(TAG_RETAINED_2),
      value = retainedText2,
      onValueChange = { retainedText2 = it },
    )
    BasicTextField(
      modifier = Modifier.testTag(TAG_RETAINED_3),
      value = retainedInt.toString(),
      onValueChange = { retainedInt = it.toInt() },
    )
  }
}

@Composable
private fun NestedRetains() {
  var retainedText1: String by retain(key = "retained1") { mutableStateOf("") }

  Column {
    BasicTextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
    )

    val nestedRegistryLevel1 = retainRetainedValuesStoreRegistry()
    nestedRegistryLevel1.LocalRetainedValuesStoreProvider("registryLevel1") {
      NestedRetainLevel1()
    }
  }
}

@Composable
private fun NestedRetainLevel1() {
  var retainedText2: String by retain(key = "retained2") { mutableStateOf("") }

  BasicTextField(
    modifier = Modifier.testTag(TAG_RETAINED_2),
    value = retainedText2,
    onValueChange = { retainedText2 = it },
  )

  val nestedRegistryLevel2 = retainRetainedValuesStoreRegistry()
  nestedRegistryLevel2.LocalRetainedValuesStoreProvider("registryLevel2") { NestedRetainLevel2() }
}

@Composable
private fun NestedRetainLevel2() {
  var retainedInt: Int by retain(key = "retainedInt") { mutableStateOf(0) }

  BasicTextField(
    modifier = Modifier.testTag(TAG_RETAINED_3),
    value = retainedInt.toString(),
    onValueChange = { retainedInt = it.toInt() },
  )
}

@Composable
private fun NestedRetainWithPushAndPop() {
  var retainedText1: String by retain(key = "retained1") { mutableStateOf("") }

  Column {
    BasicTextField(
      modifier = Modifier.testTag(TAG_RETAINED_1),
      value = retainedText1,
      onValueChange = { retainedText1 = it },
    )

    val showNestedContent = androidxRetain { mutableStateOf(false) }

    BasicText(
      text = "Hide child",
      modifier = Modifier.testTag(TAG_BUTTON_HIDE).clickable { showNestedContent.value = false },
    )

    BasicText(
      text = "Show child",
      modifier = Modifier.testTag(TAG_BUTTON_SHOW).clickable { showNestedContent.value = true },
    )

    val retainedRegistry = retainRetainedValuesStoreRegistry()
    // Keep the retained values store around even if showNestedContent becomes false
    if (showNestedContent.value) {
      retainedRegistry.LocalRetainedValuesStoreProvider("registry") { NestedRetainLevel1() }
    }
  }
}
