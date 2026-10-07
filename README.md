# Asleep SDK Android Sample

A sample application that demonstrates how to use the Asleep Android SDK.

- 100% Kotlin
- AAC ViewModel based
- Built against the latest Asleep Android SDK (3.3.1)

See the [Asleep SDK Android Docs](https://docs.asleep.ai/docs/android) for integration details.

## Standard implementation (this branch)

This branch contains the **most standard integration**.

| Topic | This branch |
|---|---|
| Authentication | `Asleep.initAsleepConfig(apiKey = ...)` — direct API Key initialization |
| Tracking | `Asleep.beginSleepTracking()` / `Asleep.endSleepTracking()` — the SDK's built-in Foreground Service |
| Callbacks | `Asleep.AsleepTrackingListener` (`onStart` / `onPerform` / `onFinish` / `onFail`) |
| Interim results | Polling with `Asleep.getCurrentSleepData()` |
| Reports | Session list and details via `Asleep.createReports()` |

## Sample branches

Each branch below starts from this one and changes **a single integration decision**, so you can diff exactly the part you need.

| Branch | What it demonstrates |
|---|---|
| [`main`](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main) (default) | The standard integration described above — API Key + `beginSleepTracking()` + polling |
| [`sample/init-startstop-polling`](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/sample/init-startstop-polling) | Driving tracking directly with `SleepTrackingManager.startSleepTracking()` / `stopSleepTracking()` |
| [`sample/init-beginend-complete-recording`](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/sample/init-beginend-complete-recording) | Keeping recording files with `CompletableAsleepTrackingListener` + `recordingPath` / `RecordingType` |
| [`sample/setup-product-beginend-polling`](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/sample/setup-product-beginend-polling) | Registering the device as a product with `Asleep.setup()` + `ProductInfo` before initialization |
| [`sample/init-appid-beginend-polling`](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/sample/init-appid-beginend-polling) | Authenticating with `appId` / `appSecret` tokens instead of an API Key |

## Features

- Requests microphone and notification permissions for sleep tracking
- Optionally requests a battery-optimization exemption
- Runs sleep tracking while showing progress on screen
- Shows the report of the session that just ended
- Browses the report list in date order, newest or oldest first

## Things to check

- If sleep tracking does not work, check the following:
    1. The device microphone works
    2. The Foreground Service notification is visible
- The battery-optimization exemption is not required, but it helps keep the device out of Doze mode.
- A meaningful report needs at least 5 minutes of tracking (10+ uploads).

## How to run

1. Clone or download this project.
2. Add your issued API Key to `local.properties` in the project root.

   ```properties
   asleep_api_key="YOUR_API_KEY"
   ```

   > `local.properties` is covered by `.gitignore`. Never commit your API Key.

3. Run in Android Studio. Gradle and Android SDK components may need to be downloaded.

### Build environment

| Item | Version |
|---|---|
| compileSdk | 34 |
| minSdk | 24 |
| targetSdk | 34 |
| Gradle | 8.7 |
| Android Gradle Plugin | 8.5.1 |
| Kotlin | 1.9.24 |
| JDK | 17 |
| Asleep SDK | 3.3.1 |

## Feedback and questions

Leave feedback or questions [here](https://docs.asleep.ai/discuss).

## License

See [here](https://docs.asleep.ai/) for the sample app license.
