package com.example.telecom

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object ReminderScheduler {

    fun scheduleReminder(
        context: Context,
        callId: Long,
        phoneNumber: String,
        callerName: String?,
        note: String?,
        reminderEpoch: Long
    ) {
        if (reminderEpoch <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
            putExtra(ReminderReceiver.EXTRA_CALL_ID, callId)
            putExtra(ReminderReceiver.EXTRA_PHONE_NUMBER, phoneNumber)
            putExtra(ReminderReceiver.EXTRA_CALLER_NAME, callerName ?: phoneNumber)
            putExtra(ReminderReceiver.EXTRA_NOTE, note)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TIME, reminderEpoch)
        }

        val requestCode = getRequestCode(callId, phoneNumber)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderEpoch, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderEpoch, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderEpoch, pendingIntent)
                }
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderEpoch, pendingIntent)
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderEpoch, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    fun cancelReminder(context: Context, callId: Long, phoneNumber: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE_REMINDER
        }
        val requestCode = getRequestCode(callId, phoneNumber)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun getRequestCode(callId: Long, phoneNumber: String): Int {
        return ((callId.hashCode() * 31) xor phoneNumber.hashCode()) and 0x7FFFFFFF
    }
}
