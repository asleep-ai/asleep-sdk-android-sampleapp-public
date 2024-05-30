## foreground service의 process 분리 작업

google의 gms, webview 등의 잦은 업데이트로 인하여 밤중 app이 종료되는 케이스를 방지하고자 foreground service의 process를 앱과 분리하는 작업을 리팩토링

1. AndroidManifest.xml의 service에 process, intent-filter의 action 추가
2. activity 프로세스와 fgs 프로세스간 통신을 위하여 aidl 파일 추가
3. 기존 activity와 fgs에서 함께 사용하였던 viewModel 분리 작업
4. 기타 작업으로 activity에서 fgs의 데이터 수신을 위한 bind, unbind 처리


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
