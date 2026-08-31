# Sample iOS App

This is a SwiftUI application that hosts the Compose Multiplatform UI from the `:sample` shared module.

## How to run

1. Open `SampleIOS.xcodeproj` in Xcode.
2. Select an iOS Simulator.
3. Run the project (Cmd+R).

The build phase contains a script that will automatically invoke the Gradle task `embedAndSignAppleFrameworkForXcode` (or standard `xcode-frameworks` build) to compile the Kotlin code into a framework whenever you build in Xcode.
