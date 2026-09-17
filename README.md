# Asleep SDK Android Sample — `sample/init-appid-beginend-polling`

> This branch is a variant of the [default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main).
> See the default branch README for the full list of variants.

> **Switching from an API Key to appId / appSecret requires provisioning.** The credentials are
> issued per contract — contact **platform-cs@asleep.ai** or your account manager to get an
> `appId` / `appSecret` before using this branch.

## What this branch demonstrates

Authenticating with **`appId` / `appSecret` tokens instead of an API Key**.

Since SDK 3.3.0, `initAsleepConfig()` provides both an API Key overload and an `appId` / `appSecret` overload. In the token flow, the SDK issues and refreshes access tokens from `appId` / `appSecret` and calls the API with them.

| Topic | default | This branch |
|---|---|---|
| Authentication | `initAsleepConfig(apiKey = ...)` | `initAsleepConfig(appId = ..., appSecret = ...)` |
| Tracking | `Asleep.beginSleepTracking()` / `endSleepTracking()` | same |
| Callbacks | `Asleep.AsleepTrackingListener` | same |
| Interim results | Polling with `Asleep.getCurrentSleepData()` | same |

## Changes from default

### `app/build.gradle`

The `ASLEEP_API_KEY` BuildConfig field is replaced with `ASLEEP_APP_ID` / `ASLEEP_APP_SECRET`.

```groovy
buildConfigField "String", "ASLEEP_APP_ID", properties['asleep.appId']
buildConfigField "String", "ASLEEP_APP_SECRET", properties['asleep.appSecret']
```

### `ui/Constants.kt`

`ASLEEP_API_KEY` → `ASLEEP_APP_ID` / `ASLEEP_APP_SECRET`

### `ui/main/AsleepViewModel.kt`, `ui/report/ReportViewModel.kt`

All three `initAsleepConfig()` call sites (`initAsleepConfig()`, `beginAutoSleepTracking()`, `ReportViewModel.initAsleepConfig()`) switch to the token overload.

```kotlin
Asleep.initAsleepConfig(
    context = applicationContext,
    appId = Constants.ASLEEP_APP_ID,
    appSecret = Constants.ASLEEP_APP_SECRET,
    userId = storedUserId,
    baseUrl = Constants.BASE_URL,
    callbackUrl = Constants.CALLBACK_URL,
    service = Constants.SERVICE_NAME,
    asleepConfigListener = ...
)
```

> Pass `isTestEnvironment = true` to connect to the test environment. The default is production.

## Switching authentication on an existing install

If you install this branch on top of an install that used different credentials (e.g. the default branch's API Key), the app tries to join with the userId stored for the previous customer and gets `11404` (Not Found).
**Clear the app data (or reinstall) and run again** to get a fresh userId.

## Setup

1. Add your issued App ID / App Secret to `local.properties` in the project root.

   ```properties
   asleep.appId="YOUR_APP_ID"
   asleep.appSecret="YOUR_APP_SECRET"
   ```

   > Write the values **with the quotes included** — they are passed straight into `buildConfigField`.
   > `local.properties` is covered by `.gitignore`. Never commit your credentials.

2. Run in Android Studio.

This branch does not use an API Key, so the `asleep_api_key` entry is not needed.

## See also

- [Asleep SDK Android Docs](https://docs.asleep.ai/docs/android)
- [Back to the default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main)
