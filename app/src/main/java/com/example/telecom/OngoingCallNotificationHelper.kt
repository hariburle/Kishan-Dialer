package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object OngoingCallNotificationHelper {
    private const val CHANNEL_ID = "ongoing_call_channel"
    private const val NOTIFICATION_ID = 9001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ongoing Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows active call status and quick call actions"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showCallNotification(context: Context, callInfo: ActiveCallInfo) {
        createNotificationChannel(context)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_IN_CALL", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val hangupIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = CallNotificationReceiver.ACTION_HANGUP
        }
        val hangupPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            hangupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleMuteIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = CallNotificationReceiver.ACTION_TOGGLE_MUTE
        }
        val toggleMutePendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            toggleMuteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleSpeakerIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = CallNotificationReceiver.ACTION_TOGGLE_SPEAKER
        }
        val toggleSpeakerPendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            toggleSpeakerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = when (callInfo.state) {
            Call.STATE_RINGING -> "Incoming call..."
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling..."
            Call.STATE_ACTIVE -> "Call in progress"
            Call.STATE_HOLDING -> "On hold"
            else -> "Ongoing call"
        }

        val isMuted = CallManager.isMuted.value
        val isSpeaker = CallManager.isSpeakerOn.value

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle(callInfo.displayName.ifBlank { callInfo.phoneNumber })
            .setContentText("$statusText • ${callInfo.phoneNumber}")
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                if (isMuted) "Unmute" else "Mute",
                toggleMutePendingIntent
            )
            .addAction(
                android.R.drawable.stat_notify_chat,
                if (isSpeaker) "Handset" else "Speaker",
                toggleSpeakerPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Hang up",
                hangupPendingIntent
            )

        // For incoming ringing call, set full screen intent to pop over lockscreen
        if (callInfo.state == Call.STATE_RINGING) {
            builder.setFullScreenIntent(contentPendingIntent, true)
        }

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, builder.build())
        } catch (e: Exception) {
            // Permission or security exception
        }
    }

    fun cancelCallNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // ignore
        }
    }
}
