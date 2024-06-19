## Separate the Foreground Service into a Process

To prevent the app from terminating, you separate the Foreground Service (FGS) into a distinct process.

By separating the process, you can maintain the sleep tracking despite updates of other programs occurring at night. (e.g. updates for GMS and WebView)

Please follow the steps below.

1. Add the process attribute and the intent-filter's action to the <service> in AndroidManifest.xml.
2. Add an AIDL file to enable communication between the activity process and the FGS process.
3. Separate the ViewModel that was previously used together in the activity and FGS.
4. Add bind and unbind handling to receive data from the FGS in the activity. For example, while sleep tracking is in progress, receive data and display it on the UI in the activity.
