## foreground service의 process 분리 작업

google의 gms, webview 등의 잦은 업데이트로 인하여 밤중 app이 종료되는 케이스를 방지하고자 foreground service의 process를 앱과 분리하는 작업

1. AndroidManifest.xml의 service에 process, intent-filter의 action 추가
2. activity 프로세스와 fgs 프로세스간 통신을 위하여 aidl 파일 추가
3. 기존 activity와 fgs에서 함께 사용하였던 viewModel 분리 작업
4. 기타 작업으로 activity에서 fgs의 데이터 수신을 위한 bind, unbind 처리
