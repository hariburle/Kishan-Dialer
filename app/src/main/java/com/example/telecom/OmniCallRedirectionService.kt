package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.util.ContactHelper

/**
 * OmniCallRedirectionService intercepts outgoing calls initiated from external interfaces—
 * such as Bluetooth vehicle infotainment systems (e.g. car head units), smartwatches,
 * voice assistants, or third-party dialers—and redirects them to WhatsApp VoIP when WhatsApp
 * is configured as the contact's preferred calling channel.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class OmniCallRedirectionService : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        val rawScheme = handle.schemeSpecificPart ?: ""
        val cleanNumber = rawScheme.replace(Regex("[^0-9+]"), "")
        val digitsOnly = cleanNumber.filter { it.isDigit() }

        if (cleanNumber.isBlank() || ContactHelper.isVoicemailNumber(this, cleanNumber)) {
            placeCallUnmodified()
            return
        }

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val globalMode = prefs.getString("whatsapp_call_mode", "ask_learn") ?: "ask_learn"

        // If user globally disabled WhatsApp calling, let cellular proceed unmodified
        if (globalMode == "never") {
            placeCallUnmodified()
            return
        }

        // Check contact-specific learned calling channel
        val learnedModes = prefs.getStringSet("learned_call_modes", emptySet()) ?: emptySet()
        val suffix10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly

        var preferredMode: String? = null
        for (entry in learnedModes) {
            val parts = entry.split(":")
            if (parts.size >= 2) {
                val numKey = parts[0]
                val mode = parts[1]
                if (numKey == cleanNumber || (suffix10.isNotEmpty() && numKey.endsWith(suffix10))) {
                    preferredMode = mode
                    break
                }
            }
        }

        val isInternational = ContactHelper.isInternationalNumber(this, cleanNumber)
        val shouldRedirectToWhatsApp = when {
            preferredMode == "whatsapp" -> true
            preferredMode == "cellular" -> false
            globalMode == "all_international" && isInternational -> true
            else -> false
        }

        if (shouldRedirectToWhatsApp) {
            Log.i(TAG, "External outgoing call for $cleanNumber redirected to WhatsApp")
            // Abort cellular network call
            cancelCall()

            // Trigger WhatsApp Call immediately
            try {
                ContactHelper.launchWhatsAppCall(applicationContext, cleanNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Direct WhatsApp launch error, dispatching notification fallback", e)
                showWhatsAppRedirectionNotification(cleanNumber)
            }
        } else {
            // Let the standard cellular call proceed
            placeCallUnmodified()
        }
    }

    private fun showWhatsAppRedirectionNotification(phoneNumber: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "call_redirection_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Outgoing Call Redirection",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for redirected car & bluetooth calls"
                }
                nm.createNotificationChannel(channel)
            }

            val waIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://call?phone=${phoneNumber.filter { it.isDigit() }}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                `package` = "com.whatsapp"
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                109,
                waIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("Connecting WhatsApp Call")
                .setContentText("Redirecting outgoing call to $phoneNumber via WhatsApp")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(902, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display redirection notification", e)
        }
    }

    companion object {
        private const val TAG = "OmniCallRedirection"
    }
}
