package com.example.telecom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.MainActivity
import com.example.R

object OngoingCallNotificationHelper {
    private const val TAG = "OngoingCallNotification"
    const val CHANNEL_ID = "ongoing_call_silent_channel_v4"
    const val NOTIFICATION_ID = 9001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ongoing Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active call status silently in status bar without popup"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(false)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun buildCallNotification(context: Context, callInfo: ActiveCallInfo): Notification {
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

        val isMuted = CallManager.isMuted.value
        val isSpeaker = CallManager.isSpeakerOn.value
        val callerTitle = callInfo.displayName.ifBlank { callInfo.phoneNumber }

        val elapsedSec = if (callInfo.state == Call.STATE_ACTIVE && callInfo.connectTimeMillis > 0) {
            (System.currentTimeMillis() - callInfo.connectTimeMillis) / 1000
        } else 0L
        val minutes = elapsedSec / 60
        val seconds = elapsedSec % 60
        val timerFormatted = String.format("%02d:%02d", minutes, seconds)

        val statusText = when (callInfo.state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling..."
            Call.STATE_ACTIVE -> "Active call • $timerFormatted"
            Call.STATE_HOLDING -> "On hold"
            else -> "Call"
        }

        val contentText = when (callInfo.state) {
            Call.STATE_ACTIVE -> "${callInfo.phoneNumber} • $timerFormatted"
            Call.STATE_RINGING -> "Incoming call • ${callInfo.phoneNumber}"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling • ${callInfo.phoneNumber}"
            else -> "${callInfo.phoneNumber} • $statusText"
        }

        val isUiInFocus = CallManager.isCallUiForegrounded
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_ongoing)
            .setContentTitle(callerTitle)
            .setContentText(contentText)
            .setSubText(if (callInfo.state == Call.STATE_ACTIVE) timerFormatted else statusText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(if (isUiInFocus) NotificationCompat.PRIORITY_MIN else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF16A34A.toInt()) // Standard active call green
            .setColorized(!isUiInFocus)
            .setSilent(isUiInFocus)
            .setOnlyAlertOnce(true)
            .setTicker(if (isUiInFocus) null else "$callerTitle: $statusText")

        // Chronometer for native ticking live elapsed time
        if (callInfo.state == Call.STATE_ACTIVE && !isUiInFocus) {
            val connectTime = if (callInfo.connectTimeMillis > 0) callInfo.connectTimeMillis else System.currentTimeMillis()
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(false)
            builder.setShowWhen(true)
            builder.setWhen(connectTime)
        } else {
            builder.setUsesChronometer(false)
            builder.setShowWhen(false)
        }

        val person = Person.Builder()
            .setName(callerTitle)
            .setImportant(true)
            .build()

        // Modern NotificationCompat.CallStyle for Android status bar banner appearance ONLY when outside app
        if (!isUiInFocus) {
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
            } catch (e: Exception) {
                Log.w(TAG, "CallStyle not applied: ${e.message}")
            }
        }

        if (callInfo.state == Call.STATE_RINGING) {
            builder.addAction(
                R.drawable.ic_call_ongoing,
                "Answer",
                answerPendingIntent
            )
            builder.addAction(
                R.drawable.ic_call_end,
                "Decline",
                hangupPendingIntent
            )
            if (!CallManager.isCallUiForegrounded) {
                builder.setFullScreenIntent(contentPendingIntent, true)
            }
        } else {
            builder.addAction(
                if (isMuted) R.drawable.ic_mic else R.drawable.ic_mic_off,
                if (isMuted) "Unmute" else "Mute",
                toggleMutePendingIntent
            )
            builder.addAction(
                R.drawable.ic_volume_up,
                if (isSpeaker) "Handset" else "Speaker",
                toggleSpeakerPendingIntent
            )
            builder.addAction(
                R.drawable.ic_call_end,
                "Hang up",
                hangupPendingIntent
            )
            if (!CallManager.isCallUiForegrounded) {
                builder.setFullScreenIntent(contentPendingIntent, true)
            }
        }

        return builder.build()
    }

    fun showCallNotification(context: Context, callInfo: ActiveCallInfo) {
        // If the user is actively viewing the dialer app, suppress popup notification
        if (CallManager.isCallUiForegrounded) {
            return
        }
        try {
            val notification = buildCallNotification(context, callInfo)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show call notification with CallStyle, falling back", e)
            try {
                val fallbackNotification = buildCallNotificationWithoutStyle(context, callInfo)
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, fallbackNotification)
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to show fallback call notification", ex)
            }
        }
    }

    private fun buildCallNotificationWithoutStyle(context: Context, callInfo: ActiveCallInfo): Notification {
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

        val isMuted = CallManager.isMuted.value
        val isSpeaker = CallManager.isSpeakerOn.value
        val callerTitle = callInfo.displayName.ifBlank { callInfo.phoneNumber }

        val elapsedSec = if (callInfo.state == Call.STATE_ACTIVE && callInfo.connectTimeMillis > 0) {
            (System.currentTimeMillis() - callInfo.connectTimeMillis) / 1000
        } else 0L
        val minutes = elapsedSec / 60
        val seconds = elapsedSec % 60
        val timerFormatted = String.format("%02d:%02d", minutes, seconds)

        val statusText = when (callInfo.state) {
            Call.STATE_RINGING -> "Incoming call"
            Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling..."
            Call.STATE_ACTIVE -> "Active call • $timerFormatted"
            Call.STATE_HOLDING -> "On hold"
            else -> "Call"
        }

        val isUiInFocus = CallManager.isCallUiForegrounded
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_call_ongoing)
            .setContentTitle(callerTitle)
            .setContentText(statusText)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(if (isUiInFocus) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFF16A34A.toInt())
            .setOnlyAlertOnce(true)

        if (callInfo.state == Call.STATE_RINGING) {
            builder.addAction(R.drawable.ic_call_ongoing, "Answer", answerPendingIntent)
            builder.addAction(R.drawable.ic_call_end, "Decline", hangupPendingIntent)
            if (!isUiInFocus) {
                builder.setFullScreenIntent(contentPendingIntent, true)
            }
        } else {
            builder.addAction(
                if (isMuted) R.drawable.ic_mic else R.drawable.ic_mic_off,
                if (isMuted) "Unmute" else "Mute",
                toggleMutePendingIntent
            )
            builder.addAction(
                R.drawable.ic_volume_up,
                if (isSpeaker) "Handset" else "Speaker",
                toggleSpeakerPendingIntent
            )
            builder.addAction(
                R.drawable.ic_call_end,
                "Hang up",
                hangupPendingIntent
            )
        }

        return builder.build()
    }

    fun cancelCallNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel call notification", e)
        }
    }
}

