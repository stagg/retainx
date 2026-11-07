# Changelog

## [Unreleased]

### Added

- Ported Compose Multiplatform retained values store infrastructure from [Circuit](https://github.com/slackhq/circuit):
  - **`RetainedValuesStoreProvider`**: A Composable wrapper that installs a `LocalRetainedValuesStore` surviving composition recreation for the lifetime of its owner. Yields to any existing non-forgetful store in the composition hierarchy, allowing platform or host-provided stores to take precedence.
  - **Automatic ViewModel Owner Resolution**: Automatic resolution of the `RetainedValuesStoreOwner` via `LocalViewModelStoreOwner` on platforms with AndroidX ViewModel support (Android, JVM/Desktop, iOS, macOS, JS, and WasmJS). Stores are retained until the backing `ViewModelStore` is cleared, working around compose-multiplatform limitations where default stores lose retained values when compositions are destroyed.
- Ported retained state APIs from Circuit, built on the first-party `RetainedValuesStore`:
  - **`retain(key)`**: Retains a value under an explicit key instead of its position.
  - **`retainSaveable`**: Retains a value and also saves it, so it's restored after process death or when nothing can retain. Supports `Saver` and `MutableState` (`stateSaver`) variants.
  - **`produceRetainState`, `collectAsRetainState`, `produceAndCollectAsRetainState`**: Retained versions of `produceState` and `collectAsState`.

### Changed

- Differences from Circuit's produce/collect APIs:
  - **`produceRetainState`** takes a `context` parameter, so the producer can run off the main dispatcher.
  - **`produceAndCollectAsRetainState`** now calls `producer` in `context` too, not just the collection.
  - A `Job` in `context` is ignored, so work is still cancelled when the content leaves composition.
