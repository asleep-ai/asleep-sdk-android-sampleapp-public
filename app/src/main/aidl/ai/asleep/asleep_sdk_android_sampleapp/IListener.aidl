// IListener.aidl
package ai.asleep.asleep_sdk_android_sampleapp;

import ai.asleep.asleep_sdk_android_sampleapp.data.ErrorCode;

interface IListener {
    void onUserIdReceived(String userId);
    void onSessionIdReceived(String sessionId);
    void onSequenceReceived(int sequence);
    void onErrorCodeReceived(in ErrorCode errorCode);
    void onStopTrackingReceived(String sessionId);
}