// Copyright (C) 2026 Slack Technologies, LLC
// SPDX-License-Identifier: Apache-2.0
package me.stagg.retainx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProduceStateScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time without a defined data source.
 *
 * [producer] is launched when [produceRetainState] enters the composition and is cancelled when
 * [produceRetainState] leaves the composition. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * By default [producer] runs on the composition's dispatcher. Pass a [context], such as a
 * background dispatcher, to run it there instead. Setting [ProduceStateScope.value] from any thread
 * is safe. Any [Job] in [context] is ignored so [producer] is still cancelled with the composition.
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainState(
  initialValue: T,
  context: CoroutineContext = EmptyCoroutineContext,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  val result = retain { mutableStateOf(initialValue) }
  LaunchedEffect(Unit) { runProducer(result, context, producer) }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1].
 *
 * [producer] is launched when [produceRetainState] enters the composition and is cancelled when
 * [produceRetainState] leaves the composition. If [key1] changes, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * By default [producer] runs on the composition's dispatcher. Pass a [context], such as a
 * background dispatcher, to run it there instead. Setting [ProduceStateScope.value] from any thread
 * is safe. Any [Job] in [context] is ignored so [producer] is still cancelled with the composition.
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainState(
  initialValue: T,
  key1: Any?,
  context: CoroutineContext = EmptyCoroutineContext,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  val result = retain { mutableStateOf(initialValue) }
  LaunchedEffect(key1) { runProducer(result, context, producer) }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1] and [key2].
 *
 * [producer] is launched when [produceRetainState] enters the composition and is cancelled when
 * [produceRetainState] leaves the composition. If [key1] or [key2] change, a running [producer]
 * will be cancelled and re-launched for the new source. [producer] should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * By default [producer] runs on the composition's dispatcher. Pass a [context], such as a
 * background dispatcher, to run it there instead. Setting [ProduceStateScope.value] from any thread
 * is safe. Any [Job] in [context] is ignored so [producer] is still cancelled with the composition.
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainState(
  initialValue: T,
  key1: Any?,
  key2: Any?,
  context: CoroutineContext = EmptyCoroutineContext,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  val result = retain { mutableStateOf(initialValue) }
  LaunchedEffect(key1, key2) { runProducer(result, context, producer) }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [key1], [key2] and [key3].
 *
 * [producer] is launched when [produceRetainState] enters the composition and is cancelled when
 * [produceRetainState] leaves the composition. If [key1], [key2] or [key3] change, a running
 * [producer] will be cancelled and re-launched for the new source. [producer] should use
 * [ProduceStateScope.value] to set new values on the returned [State].
 *
 * By default [producer] runs on the composition's dispatcher. Pass a [context], such as a
 * background dispatcher, to run it there instead. Setting [ProduceStateScope.value] from any thread
 * is safe. Any [Job] in [context] is ignored so [producer] is still cancelled with the composition.
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainState(
  initialValue: T,
  key1: Any?,
  key2: Any?,
  key3: Any?,
  context: CoroutineContext = EmptyCoroutineContext,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  val result = retain { mutableStateOf(initialValue) }
  LaunchedEffect(key1, key2, key3) { runProducer(result, context, producer) }
  return result
}

/**
 * Return an observable [snapshot][androidx.compose.runtime.snapshots.Snapshot] [State] that
 * produces values over time from [keys].
 *
 * [producer] is launched when [produceRetainState] enters the composition and is cancelled when
 * [produceRetainState] leaves the composition. If [keys] change, a running [producer] will be
 * cancelled and re-launched for the new source. [producer] should use [ProduceStateScope.value] to
 * set new values on the returned [State].
 *
 * By default [producer] runs on the composition's dispatcher. Pass a [context], such as a
 * background dispatcher, to run it there instead. Setting [ProduceStateScope.value] from any thread
 * is safe. Any [Job] in [context] is ignored so [producer] is still cancelled with the composition.
 *
 * The returned [State] conflates values; no change will be observable if [ProduceStateScope.value]
 * is used to set a value that is [equal][Any.equals] to its old value, and observers may only see
 * the latest value if several values are set in rapid succession.
 *
 * [produceRetainState] may be used to observe either suspending or non-suspending sources of
 * external data, for example:
 * ```
 * @Composable
 * fun FavoritesPresenter(favoritesRepository: FavoritesRepository): State {
 *   val state by produceRetainState<UiState<List<Person>>>(UiState.Loading, favoritesRepository) {
 *     favoritesRepository.people
 *       .map { UiState.Data(it) }
 *       .collect { value = it }
 *   }
 *   return state
 * }
 * ```
 */
@Composable
public fun <T> produceRetainState(
  initialValue: T,
  vararg keys: Any?,
  context: CoroutineContext = EmptyCoroutineContext,
  producer: suspend ProduceStateScope<T>.() -> Unit,
): State<T> {
  val result = retain { mutableStateOf(initialValue) }
  LaunchedEffect(keys = keys) { runProducer(result, context, producer) }
  return result
}

private suspend fun <T> CoroutineScope.runProducer(
  state: MutableState<T>,
  context: CoroutineContext,
  producer: suspend ProduceStateScope<T>.() -> Unit,
) {
  if (context == EmptyCoroutineContext) {
    ProduceRetainStateScopeImpl(state, coroutineContext).producer()
  } else {
    withContext(context.minusKey(Job)) {
      ProduceRetainStateScopeImpl(state, coroutineContext).producer()
    }
  }
}

private class ProduceRetainStateScopeImpl<T>(
  state: MutableState<T>,
  override val coroutineContext: CoroutineContext,
) : ProduceStateScope<T>, MutableState<T> by state {

  override suspend fun awaitDispose(onDispose: () -> Unit): Nothing {
    try {
      suspendCancellableCoroutine<Nothing> {}
    } finally {
      onDispose()
    }
  }
}
