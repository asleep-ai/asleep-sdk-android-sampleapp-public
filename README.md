# Asleep SDK Android Sample
This repository contains a sample app using Asleep SDK. It features:
- Fully Kotlin
- Using AAC-ViewModel
- Compatible with the latest Asleep Android SDK

Learn more about the integrating the SDK into your app at [Asleep SDK Android Docs](https://docs.asleep.ai/docs/android).

## Feature
- Granting microphone and notification prermissions on Sleep Tracking
- Ignoring battery optimizations permission on Sleep Tracking (optional)
- Implementing Sleep Tracking while displaying the current situation
- Displaying the just completed sleep tracking Report
- Displaying the Reports by date descending/ascending

## Considerations for Feature
- To confirm that Sleep Tracking is functioning correctly, you should check the following:

    1. The app device's microphone is working.
    2. The Service's Notification is shown.

- 'The Ignoring battery optimazations' is not required. However, it can be helpful if you have the appropriate permissions to prevent you from falling into Doze mode.
- You need to track for 40 or more uploads to obtain the valid report.

## Ruuning the sample app
- Download or clone this project to your machine
- Run the app from Andrdoid Studio. You may have to download the correct version of Gradle and Android build tools from the Android SDK Manager.

### Version
- Android SDK
    - compileSdk: 34
    - minSdk: 24
    - targetSdk: 34
- Gradle Version
    - gradle: 8.2 
    - gradle plugin: 8.2.0 (Android Stduio Hedgehog+)

## Feedback and Questions
Please send your feedback or question [here](https://docs.asleep.ai/discuss).

## License
License for the sample app can be found [here](). <!--샘플 앱 라이센스.md 추가-->
