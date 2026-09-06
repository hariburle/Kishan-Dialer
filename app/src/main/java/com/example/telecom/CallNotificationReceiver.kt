package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_HANGUP = "com.example.telecom.ACTION_HANGUP"
        const val ACTION_TOGGLE_MUTE = "com.example.telecom.ACTION_TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.example.telecom.ACTION_TOGGLE_SPEAKER"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_HANGUP -> {
                CallManager.disconnectCall()
            }
            ACTION_TOGGLE_MUTE -> {
                CallManager.toggleMute()
                CallManager.activeCall.value?.let {
                    OngoingCallNotificationHelper.showCallNotification(context, it)
                }
            }
            ACTION_TOGGLE_SPEAKER -> {
                CallManager.toggleSpeaker()
                CallManager.activeCall.value?.let {
                    OngoingCallNotificationHelper.showCallNotification(context, it)
                }
            }
        }
    }
}
