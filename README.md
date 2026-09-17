# Asleep SDK Android Sample — `sample/init-beginend-complete-recording`

> This branch is a variant of the [default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main).
> See the default branch README for the full list of variants.

> **Requires a server-side plan.** Recording files are only kept for segments the server detects,
> so your plan must include snoring / apnea detection. Contact **platform-cs@asleep.ai** to enable
> it on your plan.

## What this branch demonstrates

Keeping **audio recording files on the device during tracking and browsing them after the session**.

Recording requires passing a `recordingPath`, and `recordingPath` **requires a `CompletableAsleepTrackingListener`**. Passing `recordingPath` with a plain `AsleepTrackingListener` is rejected by the SDK with `ERR_INVALID_PARAMETER`.

| Topic | default | This branch |
|---|---|---|
| Authentication | `initAsleepConfig(apiKey = ...)` | same |
| Tracking | `Asleep.beginSleepTracking()` / `endSleepTracking()` | same (+ `recordingPath`, `recordingType`) |
| Callbacks | `Asleep.AsleepTrackingListener` | `Asleep.CompletableAsleepTrackingListener` (adds `onComplete(session)`) |
| Interim results | Polling with `Asleep.getCurrentSleepData()` | polling removed — `onComplete(session)` delivers the final report |
| Recording | none | browse saved files with `RecordingFileManager` |

### Callback order

```
onStart(sessionId) -> onPerform(seq) ... -> onFinish(sessionId) -> onComplete(session)
```

`onFinish()` only means **the session was closed** — the analysis is not done yet. The final report and the recording files that survived filtering are ready at `onComplete()`. That is also why navigation to the report screen happens in `onComplete()`.

> If the server cannot finish the analysis within 30 seconds, `onFail(ERR_COMPLETE_TIMEOUT)` arrives instead of `onComplete()`. The app returns to idle, and the report can be checked later with "View Report".

## Changes from default

### `ui/main/AsleepViewModel.kt`

- The listener becomes `Asleep.CompletableAsleepTrackingListener` with `onComplete(session: Session?)` added
- `Asleep.getCurrentSleepData()` polling is removed
- The recording path is set to `filesDir/recordings` and passed to the begin call

  ```kotlin
  Asleep.beginSleepTracking(
      asleepConfig = it,
      completableAsleepTrackingListener = completableAsleepTrackingListener,
      notificationTitle = applicationContext.getString(R.string.app_name),
      notificationText = "",
      notificationIcon = R.mipmap.ic_sampleapp,
      notificationClass = MainActivity::class.java,
      recordingPath = recordingPath,
      recordingType = recordingType
  )
  ```

- After the session ends, the saved file list is logged through `Asleep.createRecordingFileManager()`

  ```kotlin
  val recordingFileManager = Asleep.createRecordingFileManager(recordingPath)
  val segments = recordingFileManager.getAllSegments(sessionId)
  ```

  `RecordingFile` exposes `filePath`, `segmentIndex`, `maxDb`, `isSnoringDetected`, `isBreathDetected`, `timestamp`, `snoreIntensity`, and `breathSeverity`. Use `getSnoringFiles()` / `getBreathFiles()` to pick only snoring or apnea segments.

### `res/layout/activity_main.xml`, `res/values/arrays.xml`, `res/values/strings.xml`

Adds a Spinner (`sp_recording_type`) for choosing the `RecordingType` before starting a session.

### `ui/main/MainActivity.kt`

Converts the selected Spinner position into a `RecordingType` and passes it to `beginSleepTracking()`.

## RecordingType

`recordingType` narrows, **within what the server Plan allows**, which segments the app wants to keep. It only has meaning together with `recordingPath`.

| Value | Kept segments |
|---|---|
| `ALL` (default) | snoring + apnea segments |
| `SNORING_ONLY` | snoring segments only |
| `BREATH_ONLY` | apnea segments only |

## Setup

1. Add your issued API Key to `local.properties` in the project root.

   ```properties
   asleep_api_key="YOUR_API_KEY"
   ```

2. Run in Android Studio.
3. Before starting a session, choose the segments to keep with the **Recording Type** Spinner at the top of the screen.
4. After the session ends, check the saved file list in Logcat under the `AsleepViewModel` tag.

> Recording files are stored in the app's internal storage (`filesDir/recordings/audio/{sessionId}/`). The SDK fails with `ERR_INSUFFICIENT_STORAGE` when less than 200MB is free at session start.

## See also

- [Asleep SDK Android Docs](https://docs.asleep.ai/docs/android)
- [Back to the default branch](https://github.com/asleep-ai/asleep-sdk-android-sampleapp-public/tree/main)
