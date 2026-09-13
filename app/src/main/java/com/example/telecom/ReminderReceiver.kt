package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "call_followup_reminders"
        const val ACTION_FIRE_REMINDER = "com.example.telecom.ACTION_FIRE_REMINDER"
        const val ACTION_DISMISS_REMINDER = "com.example.telecom.ACTION_DISMISS_REMINDER"
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME"
        const val EXTRA_NOTE = "EXTRA_NOTE"
        const val EXTRA_REMINDER_TIME = "EXTRA_REMINDER_TIME"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (action == ACTION_DISMISS_REMINDER) {
            val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
            if (notifId != 0) {
                notifManager.cancel(notifId)
            }
            return
        }

        if (action == ACTION_FIRE_REMINDER) {
            val callId = intent.getLongExtra(EXTRA_CALL_ID, 0L)
            val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
            val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: phoneNumber
            val note = intent.getStringExtra(EXTRA_NOTE)

            val notifId = (phoneNumber.hashCode() and 0x7FFFFFFF) + 2000

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Follow-up Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for scheduled follow-up call reminders and post-call notes"
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(true)
                }
                notifManager.createNotificationChannel(channel)
            }

            // Tap on notification: opens Recents tab with highlight on that number
            val viewIntent = Intent(context, MainActivity::class.java).apply {
                this.action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("EXTRA_INITIAL_TAB", 1)
                putExtra("EXTRA_NAV_TAB", "RECENTS")
                putExtra("EXTRA_NAV_TAB_INDEX", 1)
                putExtra("EXTRA_HIGHLIGHT_NUMBER", phoneNumber)
            }
            val viewPendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action: Call Back
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneNumber")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val callPendingIntent = PendingIntent.getActivity(
                context,
                notifId + 1,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action: Dismiss
            val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
                this.action = ACTION_DISMISS_REMINDER
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 2,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val displayTitle = "Follow-up: $callerName"
            val displayBody = if (!note.isNullOrBlank()) {
                "Note: $note"
            } else {
                "Time to follow up with $callerName ($phoneNumber)"
            }

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(displayTitle)
                .setContentText(displayBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(displayBody).setSummaryText("Call Reminder"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(defaultSoundUri)
                .setAutoCancel(true)
                .setContentIntent(viewPendingIntent)
                .addAction(android.R.drawable.ic_menu_call, "Call Back", callPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

            notifManager.notify(notifId, builder.build())
        }
    }
}
