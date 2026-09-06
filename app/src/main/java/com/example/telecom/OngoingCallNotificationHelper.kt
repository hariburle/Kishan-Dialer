package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
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
                description = "Shows active call status, ongoing timer, and quick call actions"
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

        val answerIntent = Intent(context, CallNotificationReceiver::class.java).apply {
            action = CallNotificationReceiver.ACTION_ANSWER
        }
        val answerPendingIntent = PendingIntent.getBroadcast(
            context,
            4,
            answerIntent,
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
        val callerTitle = callInfo.displayName.ifBlank { callInfo.phoneNumber }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle(callerTitle)
            .setContentText("$statusText • ${callInfo.phoneNumber}")
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTicker("$callerTitle: $statusText")

        // Ongoing Call Timer on Status Bar & Notification
        if (callInfo.state == Call.STATE_ACTIVE) {
            val connectTime = if (callInfo.connectTimeMillis > 0) callInfo.connectTimeMillis else System.currentTimeMillis()
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(false)
            builder.setShowWhen(true)
            builder.setWhen(connectTime)
            builder.setContentText(callInfo.phoneNumber)
        } else {
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
        }

        // Modern NotificationCompat.CallStyle for native dialer status bar appearance
        val person = Person.Builder()
            .setName(callerTitle)
            .setImportant(true)
            .build()

        try {
            if (callInfo.state == Call.STATE_RINGING) {
                val callStyle = NotificationCompat.CallStyle.forIncomingCall(
                    person,
                    hangupPendingIntent,
                    answerPendingIntent
                )
                builder.setStyle(callStyle)
            } else {
                val callStyle = NotificationCompat.CallStyle.forOngoingCall(
                    person,
                    hangupPendingIntent
                )
                builder.setStyle(callStyle)
            }
        } catch (_: Exception) {
            // Fallback to standard action buttons if CallStyle is unavailable
        }

        if (callInfo.state == Call.STATE_RINGING) {
            builder.addAction(
                android.R.drawable.ic_menu_call,
                "Answer",
                answerPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Decline",
                hangupPendingIntent
            )
            builder.setFullScreenIntent(contentPendingIntent, true)
        } else {
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                if (isMuted) "Unmute" else "Mute",
                toggleMutePendingIntent
            )
            builder.addAction(
                android.R.drawable.stat_notify_chat,
                if (isSpeaker) "Handset" else "Speaker",
                toggleSpeakerPendingIntent
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Hang up",
                hangupPendingIntent
            )
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
