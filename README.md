# retainx
Extensions for Jetpack [Compose retain](https://developer.android.com/reference/kotlin/androidx/compose/runtime/retain/package-summary), ported from [Circuit's](https://github.com/slackhq/circuit) `circuit-retained` and rebuilt on the first-party retain APIs.

## Artifacts

### `me.stagg.retainx:retainx`
The retain APIs from `circuit-retained` that only depend on the Compose runtime.

- `retain(key)` - retain under an explicit key instead of the call's position.
- `retainSaveable` - retained and saved, so it comes back after process death too.
- `produceRetainState`, `collectAsRetainState`, `produceAndCollectAsRetainState` - retained versions of `produceState`/`collectAsState`. Pass a `context` to move work off main.

### `me.stagg.retainx:retainx-store`
Circuit's `RetainedValuesStoreProvider`, so retain works the same everywhere.

On Android, Compose UI's store keeps retained values when the composition is recreated. Everywhere else the default store drops them, just like `remember`. Wrap your root content to fix that:

```kotlin
RetainedValuesStoreProvider {
  App()
}
```

- It uses `LocalViewModelStoreOwner`, so values live as long as that does. Android, JVM, iOS, macOS, JS and Wasm only.
- Pass a `RetainedValuesStoreOwner` to control the lifetime yourself (and `dispose()` it when you're done). Needed on Linux, Windows, tvOS and watchOS.
- On Android it steps aside for Compose UI's store, so it's safe to wrap shared code.
