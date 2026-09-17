# Asleep SDK Android Sample — `sample/init-startstop-polling`

> This branch is a variant of the [default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main).
> See the default branch README for the full list of variants.

## What this branch demonstrates

Driving sleep tracking **directly through `SleepTrackingManager`**.

On the default branch, `Asleep.beginSleepTracking()` / `endSleepTracking()` let the SDK spin up its built-in Foreground Service and manage the session lifetime for you. This branch instead creates the manager with `Asleep.createSleepTrackingManager()` and starts/stops the session with `startSleepTracking()` / `stopSleepTracking()` — which means the app has to bring its own Foreground Service to run them in.

| Topic | default | This branch |
|---|---|---|
| Authentication | `initAsleepConfig(apiKey = ...)` | same |
| Tracking | `Asleep.beginSleepTracking()` / `endSleepTracking()` | `SleepTrackingManager.startSleepTracking()` / `stopSleepTracking()` |
| Callbacks | `Asleep.AsleepTrackingListener` | `SleepTrackingManager.TrackingListener` |
| Interim results | Polling with `Asleep.getCurrentSleepData()` | Polling with `SleepTrackingManager.requestAnalysis()` |
| Foreground Service | SDK built-in | app-implemented `AsleepService` in a dedicated `:RecordingService` process (this branch) |

> **Note**: `SleepTrackingManager` must run inside a Foreground Service that your app provides — without one the system reclaims the process (and the microphone) as soon as the app goes to the background. This branch runs it in a separate process, mirroring how the SDK's built-in service is arranged, so recording survives even when the UI process is reclaimed. The cost is that the UI can no longer share state in memory with the session: it binds to the service and receives updates over AIDL callbacks.

## How the pieces fit together

```
main process                          :RecordingService process
-------------------------------       --------------------------------
ui/main/MainActivity                   service/AsleepService
ui/main/AsleepViewModel                service/AsleepViewModel
        |                                      |
        |  startForegroundService(ACTION_*)    |
        | -----------------------------------> |  SleepTrackingManager
        |                                      |
        |  bindService() + IListener callbacks |
        | <----------------------------------- |
```

## Changes from default

### `service/AsleepService.kt` (new)

A `LifecycleService` that hosts the tracking session.

- `onCreate()` creates a silent notification channel (`IMPORTANCE_LOW`) and calls `startForeground()` with `FOREGROUND_SERVICE_TYPE_MICROPHONE`; tapping the notification opens `MainActivity`.
- `onStartCommand()` handles `ACTION_START_TRACKING`, `ACTION_STOP_TRACKING` and `ACTION_STOP_AUTO_TRACKING`, and returns `START_STICKY`. A restart with a null intent means the process was killed mid-session, so it calls `continueTracking()`.
- `isAsleepServiceRunning()` lets the UI process find out whether a session is still running when the user re-enters the app.
- `onBind()` returns the `IAsleepService` binder so the UI can subscribe to updates.
- When auto tracking ends while the app is off screen, the service starts `MainActivity` with the finished session id so the user lands on the report.

Manifest entry:

```xml
<service
    android:name=".service.AsleepService"
    android:enabled="true"
    android:exported="false"
    android:process=":RecordingService"
    android:foregroundServiceType="microphone"
    android:stopWithTask="false">
    <intent-filter>
        <action android:name="ai.asleep.asleep_sdk_android_sampleapp.IAsleepService" />
    </intent-filter>
</service>
```

### `service/AsleepViewModel.kt` (new)

The service-side ViewModel. It runs in the `:RecordingService` process and owns every SDK call.

- `Asleep.AsleepTrackingListener` (`onStart` / `onPerform` / `onFinish` / `onFail`) → `SleepTrackingManager.TrackingListener` (`onCreate` / `onUpload` / `onClose` / `onFail`)
  - `onCreate()` has no sessionId argument. Read it from `getTrackingStatus().sessionId`.
- `beginSleepTracking()` → `startSleepTracking()`

  ```kotlin
  _sleepTrackingManager = Asleep.createSleepTrackingManager(_asleepConfig, trackingListener)
  _sleepTrackingManager?.startSleepTracking()
  ```

- `endSleepTracking()` → `stopSleepTracking()` calls `_sleepTrackingManager?.stopSleepTracking()`
- Interim results move from `Asleep.getCurrentSleepData()` to `requestAnalysis()`
  - `Asleep.getCurrentSleepData()` works through the SDK's built-in Foreground Service binder, so it cannot be used in this configuration.
- Progress is judged by whether `getTrackingStatus().sessionId` exists, instead of `Asleep.isSleepTrackingAlive()`
- `connectSleepTracking()` (re-attaching to the SDK's Foreground Service) is replaced by `continueTracking()`, which rebuilds the config with `Asleep.getSavedAsleepConfig()` when `Asleep.hasUnfinishedSession()` reports one.
- Results are pushed to the UI process through an `IListener` AIDL callback held in a `RemoteCallbackList`.

### `aidl/` and `data/` (new)

`IAsleepService` (register/unregister a listener) and `IListener` (`onUserIdReceived`, `onSessionIdReceived`, `onSequenceReceived`, `onCurrentSleepDataReceived`, `onErrorCodeReceived`, `onStopTrackingReceived`), plus the `ErrorCode` and `CurrentSleepData` parcelables they carry. `Session` is not Parcelable, so `CurrentSleepData` carries only the latest sleep and snoring stage.

### `ui/main/AsleepViewModel.kt`

- No longer touches `SleepTrackingManager`. `startSleepTracking()` / `stopSleepTracking()` send intents to `AsleepService` and the state machine advances on AIDL callbacks.
- It still calls `initAsleepConfig()` in the UI process, but only to obtain the Asleep user id and enable the start button — the service initializes its own config in its own process.
- `isTrackingAlive()` is answered by `AsleepService.isAsleepServiceRunning()`, since the manager is out of reach.
- Callback bodies hand their work to the main thread before touching state, so the service process is never blocked waiting on the UI process.

### `ui/main/MainActivity.kt`

- On entry it checks `AsleepService.isAsleepServiceRunning()` and binds to a session that is already running, instead of always starting from initialization.
- A `reportingSessionId` extra (set by the service after auto tracking) opens `ReportActivity` directly.

### `ui/autotracking/AutoTrackingActivity.kt`

- When the alarm fires, this activity only starts `AsleepService` with `ACTION_START_TRACKING` / `ACTION_STOP_AUTO_TRACKING` and finishes right away. It exists purely to give the app a visible component from which a microphone-typed Foreground Service may be started.
- The measurement continues in the service process, so the app no longer has to be handed over to `MainActivity` and kept on screen.

## Setup

Same as the default branch.

1. Add your issued API Key to `local.properties` in the project root.

   ```properties
   asleep_api_key="YOUR_API_KEY"
   ```

2. Run in Android Studio.

## See also

- [Asleep SDK Android Docs](https://docs.asleep.ai/docs/android)
- [Back to the default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main)
