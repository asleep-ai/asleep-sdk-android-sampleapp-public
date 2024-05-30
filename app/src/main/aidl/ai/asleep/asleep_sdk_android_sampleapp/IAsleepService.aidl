// IAsleepService.aidl
package ai.asleep.asleep_sdk_android_sampleapp;

import ai.asleep.asleep_sdk_android_sampleapp.IListener;

interface IAsleepService {
    void registerListener(IListener listener);
    void unregisterListener(IListener listener);
}