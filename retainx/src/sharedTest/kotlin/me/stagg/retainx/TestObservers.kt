// Copyright (C) 2026 Josh Stagg
// SPDX-License-Identifier: MIT
package me.stagg.retainx

import androidx.compose.runtime.retain.RetainObserver
import androidx.compose.runtime.saveable.Saver

/** How many times each [RetainObserver] callback ran. */
internal data class Counts(
  val retained: Int = 0,
  val entered: Int = 0,
  val exited: Int = 0,
  val retired: Int = 0,
  val unused: Int = 0,
)

/** Records its [RetainObserver] callbacks in [counts]. */
internal open class Tracker(val id: Int = 0) : RetainObserver {
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
    /** Never saves, so only the retained store can bring a [Tracker] back. */
    val NeverSaver = Saver<Tracker, Int>(save = { null }, restore = { null })
  }
}
