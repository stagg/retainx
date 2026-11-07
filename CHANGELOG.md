# Changelog

## [Unreleased]

### Added

- `retainx`, ported from the retained state APIs in Circuit:
  - **`retain(key)`**: Retains a value under an explicit key instead of its position.
  - **`retainSaveable`**: Retains a value and also saves it, so it's restored after process death or when nothing can retain. Supports `Saver` and `MutableState` (`stateSaver`) variants.
  - **`produceRetainState`, `collectAsRetainState`, `produceAndCollectAsRetainState`**: Retained versions of `produceState` and `collectAsState`.
- `retainx-store`, ported from Circuit's retained values store infrastructure:
  - **`RetainedValuesStoreProvider`**: Wrap your root content in it to get Android's retain behavior everywhere. On Android, Compose UI's store keeps retained values when the composition is recreated. Other platforms' default store drops them, like `remember`. The provider installs a store tied to a `RetainedValuesStoreOwner` to fill that gap, and steps aside when a store that retains is already installed, so on Android it does nothing.
  - **Automatic ViewModel owner**: Without an explicit owner, the store lives as long as the `LocalViewModelStoreOwner`, on platforms with AndroidX ViewModel (Android, JVM, iOS, macOS, JS and WasmJS). Pass an owner explicitly elsewhere, or to control the lifetime yourself.

### Changed

- Differences from Circuit's produce/collect APIs:
  - **`produceRetainState`** takes a `context` parameter, so the producer can run off the main dispatcher.
  - **`produceAndCollectAsRetainState`** now calls `producer` in `context` too, not just the collection.
  - A `Job` in `context` is ignored, so work is still cancelled when the content leaves composition.
