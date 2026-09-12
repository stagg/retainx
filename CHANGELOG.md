# Changelog

## [Unreleased]

### Added

- Ported Compose Multiplatform retained values store infrastructure from [Circuit](https://github.com/slackhq/circuit):
  - **`RetainedValuesStoreProvider`**: A Composable wrapper that installs a `LocalRetainedValuesStore` surviving composition recreation for the lifetime of its owner. Yields to any existing non-forgetful store in the composition hierarchy, allowing platform or host-provided stores to take precedence.
  - **Automatic ViewModel Owner Resolution**: Automatic resolution of the `RetainedValuesStoreOwner` via `LocalViewModelStoreOwner` on platforms with AndroidX ViewModel support (Android, JVM/Desktop, iOS, macOS, JS, and WasmJS). Stores are retained until the backing `ViewModelStore` is cleared, working around compose-multiplatform limitations where default stores lose retained values when compositions are destroyed.
