package com.example.telecom.voip

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import android.util.Log

/**
 * Standard communication channels supported by the extensible dialing engine.
 */
enum class VoipChannel(val id: String, val displayName: String, val scheme: String) {
    CELLULAR("cellular", "Cellular (Carrier GSM)", "tel"),
    WHATSAPP("whatsapp", "WhatsApp Voice", "whatsapp"),
    WHATSAPP_BUSINESS("whatsapp_business", "WhatsApp Business", "whatsapp_biz"),
    SIP("sip", "SIP / VoIP Endpoint", "sip")
}

/**
 * Abstract call endpoint interface representing a communication provider.
 */
interface VoipCallEndpoint {
    val channel: VoipChannel
    fun isAvailable(context: Context): Boolean
    fun launchCall(context: Context, phoneNumber: String, reason: String? = null): Boolean
}

/**
 * Native Android Telephony (GSM / LTE / 5G) Endpoint.
 */
class GsmTelephonyEndpoint : VoipCallEndpoint {
    override val channel: VoipChannel = VoipChannel.CELLULAR

    override fun isAvailable(context: Context): Boolean {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecomManager != null
    }

    override fun launchCall(context: Context, phoneNumber: String, reason: String?): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+*#]"), "")
        if (cleanNumber.isBlank()) return false
        val uri = Uri.parse("tel:${Uri.encode(cleanNumber)}")
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: SecurityException) {
            val fallback = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
            true
        } catch (e: Exception) {
            Log.e("GsmTelephonyEndpoint", "Failed to launch GSM call", e)
            false
        }
    }
}

/**
 * WhatsApp Voice Call Endpoint.
 */
class WhatsAppVoiceEndpoint : VoipCallEndpoint {
    override val channel: VoipChannel = VoipChannel.WHATSAPP

    override fun isAvailable(context: Context): Boolean {
        return isPackageInstalled(context, "com.whatsapp")
    }

    override fun launchCall(context: Context, phoneNumber: String, reason: String?): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").trimStart('+')
        if (cleanNumber.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://send?phone=$cleanNumber")
                `package` = "com.whatsapp"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("WhatsAppVoiceEndpoint", "Failed to launch WhatsApp call", e)
            false
        }
    }

    companion object {
        fun isPackageInstalled(context: Context, packageName: String): Boolean {
            return try {
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
}

/**
 * WhatsApp Business Voice / Messaging Endpoint.
 */
class WhatsAppBusinessEndpoint : VoipCallEndpoint {
    override val channel: VoipChannel = VoipChannel.WHATSAPP_BUSINESS

    override fun isAvailable(context: Context): Boolean {
        return WhatsAppVoiceEndpoint.isPackageInstalled(context, "com.whatsapp.w4b")
    }

    override fun launchCall(context: Context, phoneNumber: String, reason: String?): Boolean {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "").trimStart('+')
        if (cleanNumber.isBlank()) return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://send?phone=$cleanNumber")
                `package` = "com.whatsapp.w4b"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("WhatsAppBusinessEndpoint", "Failed to launch WhatsApp Business call", e)
            false
        }
    }
}

/**
 * Extensible SIP VoIP Endpoint (future-proofed for custom SIP accounts).
 */
class SipVoipEndpoint : VoipCallEndpoint {
    override val channel: VoipChannel = VoipChannel.SIP

    override fun isAvailable(context: Context): Boolean = false // Available when SIP provider is configured

    override fun launchCall(context: Context, phoneNumber: String, reason: String?): Boolean {
        val uri = Uri.parse("sip:$phoneNumber")
        val intent = Intent(Intent.ACTION_CALL, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("SipVoipEndpoint", "Failed to launch SIP call", e)
            false
        }
    }
}

/**
 * Central Call Routing Engine that abstracts provider resolution.
 */
object VoipCallRouter {
    private val endpoints = listOf(
        GsmTelephonyEndpoint(),
        WhatsAppVoiceEndpoint(),
        WhatsAppBusinessEndpoint(),
        SipVoipEndpoint()
    )

    fun getAvailableEndpoints(context: Context): List<VoipCallEndpoint> {
        return endpoints.filter { it.channel == VoipChannel.CELLULAR || it.isAvailable(context) }
    }

    fun dispatchCall(
        context: Context,
        target: String,
        preferredChannel: VoipChannel = VoipChannel.CELLULAR,
        reason: String? = null
    ): Boolean {
        val endpoint = endpoints.firstOrNull { it.channel == preferredChannel && it.isAvailable(context) }
            ?: endpoints.first { it.channel == VoipChannel.CELLULAR }
        return endpoint.launchCall(context, target, reason)
    }
}
